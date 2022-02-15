package com.playsql.psea.impl;

/*
 * #%L
 * Play SQL Exports
 * %%
 * Copyright (C) 2016 - 2022 Requirement Yogi S.A.S.U.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.google.common.collect.Lists;
import com.playsql.psea.api.ExcelImportConsumer;
import com.playsql.psea.api.PSEAImportException;
import com.playsql.psea.api.PseaService;
import com.playsql.psea.api.WorkbookAPI;
import com.playsql.psea.utils.Utils.Clock;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class PseaServiceImpl implements PseaService {

    private final static Logger LOG = LoggerFactory.getLogger(PseaServiceImpl.class);
    private static final String FILE_PREFIX = "excel-export-";
    private static final String FILE_EXTENSION = ".xlsx";

    public File export(Consumer<WorkbookAPI> f) {
        SXSSFWorkbook xlWorkbook = null;
        try {
            xlWorkbook = new SXSSFWorkbook(null, 1000, false);
            xlWorkbook.setCompressTempFiles(false);
            WorkbookAPI workbook = new WorkbookAPIImpl(xlWorkbook);
            f.accept(workbook);

            File file = File.createTempFile(FILE_PREFIX, FILE_EXTENSION);
            file.deleteOnExit();
            try (FileOutputStream fileOut = new FileOutputStream(file)) {
                xlWorkbook.write(fileOut);
            }
            return file;
        } catch (IOException e) {
            throw new RuntimeException("Error while writing an Excel file to disk", e);
        } finally {
            if (xlWorkbook != null) {
                try {
                    xlWorkbook.dispose();
                    xlWorkbook.close();
                } catch (IOException e) {
                    LOG.error("Error while closing an Excel file that we were creating", e);
                }
            }
        }
    }

    @Override
    public boolean deleteFile(@Nullable File file) {
        if (file != null
                && file.exists()
                && file.getName().contains(FILE_PREFIX)
                && file.getName().endsWith(FILE_EXTENSION)) {
            return file.delete();
        }
        return false;
    }

    public void extract(PseaInput workbookFile, ExcelImportConsumer rowConsumer) throws OutOfMemoryError, PSEAImportException {
        Clock clock = Clock.start("Reading Excel file " + workbookFile.getFileName() + " - ");

        // 3 ways to skip parts of the spreadsheet
        Integer maxRows = rowConsumer.getMaxRows();
        String focusedSheet = rowConsumer.getFocusedSheet();
        Integer focusedRow = rowConsumer.getFocusedRow();

        // try reading inputstream
        Workbook workbook = null;
        try {

            if (workbookFile instanceof PseaFileInput) {
                workbook = WorkbookFactory.create(((PseaFileInput) workbookFile).getFile());
            } else if (workbookFile instanceof PseaInputStream) {
                workbook = WorkbookFactory.create(((PseaInputStream) workbookFile).getInputStream());
            } else {
                throw new RuntimeException("Unknown PseaInput class: " + workbookFile);
            }

            // apache poi representation of a .xls excel file
            // formula eveluator for workbook
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            // clear cached values
            evaluator.clearAllCachedResultValues();
            // ignore cross workbook references (formula referencing external workbook cells)
            evaluator.setIgnoreMissingWorkbooks(true);
            // iterator on sheets
            Iterator<Sheet> sheetIterator = workbook.sheetIterator();

            LOG.debug(clock.time("Done reading the file"));

            while (sheetIterator.hasNext()) {
                Sheet sheet = sheetIterator.next();
                String sheetName = sheet.getSheetName();
                final int headerRowNum = sheet.getFirstRowNum();
                if (!rowConsumer.isSheetActive(sheetName)) {
                    continue;
                }
                if (focusedSheet != null && !sheetName.equals(focusedSheet)) {
                    // We push the sheet without providing the cells, because we want the 'excel tab' to display, but only
                    // the focused one to display with cells
                    rowConsumer.consumeNewSheet(sheetName, null);
                    continue;
                }

                // If the focusedSheet is not specified, then we process at least the headerRow.
                Row row = sheet.getRow(headerRowNum);
                // If there is no first row, then there is no data on this sheet. Skip it.
                if (row != null) {
                    short firstCellNum = row.getFirstCellNum();
                    short lastCellNum = row.getLastCellNum();
                    List<String> headers = readRow(row, firstCellNum, lastCellNum, evaluator);
                    if (headers == null) {
                        throw new RuntimeException("The header row is empty for sheet '" + sheetName + "'");
                    }
                    rowConsumer.consumeNewSheet(sheetName, headers);

                    // here we need to process many rows
                    int start = headerRowNum + 1;
                    if (focusedRow != null) {
                        // focusedRow is 1-based, because it is extracted from rowNum, whereas 'start' or 'i' is 0-based
                        // And we want the row before focusedRow
                        start = Math.max(focusedRow - 2, start);
                    }
                    int end = sheet.getLastRowNum();
                    if (maxRows != null) {
                        end = Math.min(start + maxRows - 1, end);
                    }
                    for (int i = start ; i <= end ; i++) {
                        int rowNum = i + 1;
                        boolean isFocused = focusedRow != null && focusedRow == rowNum;
                        rowConsumer.consumeRow(isFocused, rowNum, readRow(sheet.getRow(i), firstCellNum, lastCellNum, evaluator));
                    }
                    rowConsumer.endOfSheet(sheetName);
                    LOG.debug(clock.time("Done reading one sheet"));
                }
            }
            rowConsumer.endOfWorkbook();
        } catch (IOException e) {
            throw new PSEAImportException(workbookFile, e);
        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException e) {
                    LOG.error("Error while closing the Excel file that we were reading", e);
                }
            }
        }
    }

    private List<String> readRow(Row row, short firstCellNum, short lastCellNum, FormulaEvaluator evaluator) {
        if (row != null) {
            List<String> values = Lists.newArrayList();
            for (int i = firstCellNum ; i < lastCellNum ; i++) {
                Cell cell = row.getCell(i);
                if (cell == null) {
                    values.add(null);
                } else {
                    values.add(computeCellValue(cell, evaluator));
                }
            }
            return values;
        } else {
            return null;
        }
    }

    private String computeCellValue(Cell cell, FormulaEvaluator evaluator){
        Object cellValue;
        try {
            CellValue computedFormulaValue = evaluator.evaluate(cell) ;
            if(computedFormulaValue != null){
                switch (computedFormulaValue.getCellType()) {
                    case NUMERIC:
                        cellValue = computedFormulaValue.getNumberValue();
                        break;
                    case STRING:
                        // TODO We could get the RTF to get bolds and similar, but it throws an exception if it's not text
                        // RichTextString richValue = cell.getRichStringCellValue();
                        cellValue = computedFormulaValue.getStringValue();
                        break;
                    case BOOLEAN:
                        cellValue = computedFormulaValue.getBooleanValue();
                        break;
                    case BLANK:
                        cellValue = "";
                        break;
                    case ERROR:
                        cellValue = computedFormulaValue.formatAsString();
                        break;
                    default:
                        cellValue = null;
                        break;
                }
            } else {
                cellValue = null;
            }
        } catch (RuntimeException ex) {
            cellValue = cell.getCellFormula();
        }
        return (cellValue == null) ? null : Objects.toString(cellValue);
    }

}
