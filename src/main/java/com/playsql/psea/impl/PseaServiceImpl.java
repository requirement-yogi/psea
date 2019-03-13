package com.playsql.psea.impl;

/*
 * #%L
 * Play SQL Exports
 * %%
 * Copyright (C) 2016 Play SQL S.A.S.U.
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
import com.playsql.psea.api.*;
import com.playsql.psea.api.PSEAFlowControlException;
import com.playsql.psea.utils.Utils.Clock;
import org.apache.log4j.Logger;
import org.apache.poi.ss.formula.eval.NotImplementedException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class PseaServiceImpl implements PseaService {

    private final static Logger LOG = Logger.getLogger(PseaServiceImpl.class);

    public File export(Consumer<WorkbookAPI> f) {
        XSSFWorkbook xlWorkbook = new XSSFWorkbook();
        WorkbookAPI workbook = new WorkbookAPIImpl(xlWorkbook);
        f.accept(workbook);

        try {
            File file = File.createTempFile("excel-export-", ".xlsx");
            file.deleteOnExit();
            FileOutputStream fileOut = new FileOutputStream(file);
            xlWorkbook.write(fileOut);
            fileOut.close();
            return file;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void extract(PseaInput workbookFile, ExcelImportConsumer rowConsumer) {
        Clock clock = Clock.start("Reading Excel file " + workbookFile.getFileName() + " - ");

        // 3 ways to skip parts of the spreadsheet
        Integer maxRows = rowConsumer.getMaxRows();
        String focusedSheet = rowConsumer.getFocusedSheet();
        Integer focusedRow = rowConsumer.getFocusedRow();

        // try reading inputstream
        try {

            Workbook workbook;
            if (workbookFile instanceof PseaFileInput) {
                workbook = WorkbookFactory.create(((PseaFileInput) workbookFile).getFile());
            } else if (workbookFile instanceof PseaInputStream) {
                workbook = WorkbookFactory.create(((PseaInputStream) workbookFile).getInputStream());
            } else {
                throw new NotImplementedException("Unknown PseaInput class: " + workbookFile);
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
                    rowConsumer.consumeNewSheet(sheetName, 0, null);
                    continue;
                }

                // If the focusedSheet is not specified, then we process at least the headerRow.
                Row row = sheet.getRow(headerRowNum);
                short firstCellNum = row.getFirstCellNum();
                short lastCellNum = row.getLastCellNum();
                List<String> headers = readRow(row, firstCellNum, lastCellNum, evaluator);
                if (headers == null) {
                    throw new RuntimeException("The header row is empty for sheet '" + sheetName + "'");
                }
                rowConsumer.consumeNewSheet(sheetName, headerRowNum, headers);

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
                LOG.debug(clock.time("Done reading one sheet"));
            }
        } catch (OutOfMemoryError oome) {
            // NO_RELEASE
        } catch (PSEAFlowControlException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("An error occured when trying to parse the file: " + workbookFile.getFileName(), ex);
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
                    case Cell.CELL_TYPE_NUMERIC:
                        cellValue = computedFormulaValue.getNumberValue();
                        break;
                    case Cell.CELL_TYPE_STRING:
                        cellValue = computedFormulaValue.getStringValue();
                        break;
                    case Cell.CELL_TYPE_BOOLEAN:
                        cellValue = computedFormulaValue.getBooleanValue();
                        break;
                    case Cell.CELL_TYPE_BLANK:
                        cellValue = "";
                        break;
                    case Cell.CELL_TYPE_ERROR:
                        cellValue = computedFormulaValue.formatAsString();
                        break;
                    default:
                        cellValue = null;
                        break;
                }
            } else {
                cellValue = null;
            }
        } catch (NotImplementedException ex) {
            cellValue = cell.getCellFormula();
        }
        return (cellValue == null) ? null : Objects.toString(cellValue);
    }

}
