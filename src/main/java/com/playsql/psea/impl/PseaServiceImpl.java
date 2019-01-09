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
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;

public class PseaServiceImpl implements PseaService {

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

    public void extract(InputStream stream, String fileName, ExcelImportConsumer rowConsumer){

        // no data to parse
        if (fileName == null || stream == null)
            return;

        Map<String, Object> config = checkNotNull(rowConsumer.getParseConfiguration());

        // first row skipping strategy
        // max rows to be processed per sheet
        Integer maxRows = (Integer) config.get("max");

        // second row skipping strategy (priority on max)
        // variables
        Object[] focusedElements = (Object[]) config.get("focusedElements");
        String focusedSheet = focusedElements == null ? null : (String) focusedElements[0];
        Integer focusedRow = focusedElements == null ? null : (Integer) focusedElements[1];

        // names of the sheets to skip
        List<String> inactiveSheets = Lists.newArrayList((String[]) checkNotNull(config.get("inactiveSheets")));

        // try reading inputstream
        try  {

            ImportableWorkbookAPI workbook = () -> fileName;

            // apache poi representation of a .xls excel file
            Workbook excelWorkbook = WorkbookFactory.create(stream);
            // iterator on sheets
            Iterator<Sheet> sheetIterator = excelWorkbook.sheetIterator();
            // for each sheet
            while (sheetIterator.hasNext()) {

                Sheet sheet = sheetIterator.next();
                String sheetName = sheet.getSheetName();

                // if the current sheet need to be skipped
                if (inactiveSheets.contains(sheetName))
                    continue;

                // metadata of the current sheet
                ImportableSheet sheetMetadata = new ImportableSheet() {
                    @Override
                    public ImportableWorkbookAPI getWorkbookAPI() {
                        // store workbook definition
                        return workbook;
                    }

                    @Override
                    public String getName() {
                        // store the name of the current sheet
                        return sheetName;
                    }
                };

                // iterator on rows in a sheet
                Iterator<org.apache.poi.ss.usermodel.Row> rowIterator = sheet.rowIterator();

                int rowCount = 0;
                // for each row
                while (rowIterator.hasNext()) {
                    //increment rowCount
                    rowCount++;

                    Row row = rowIterator.next();
                    final int rowNum = row.getRowNum();

                    // default row skipping strategy on current sheet
                    if (!sheetName.equals(focusedSheet)) {
                        // if the current row need to be skipped
                        if (maxRows != null && rowCount > maxRows) {
                            // skipping all rows with index greater than maxRows
                            rowIterator.forEachRemaining(skippedRow -> {}); // TODO Why is that necessary? I think it could be removed
                            continue; // TODO Why is that necessary? I think it could be "break;"
                        }
                    }
                    // The current sheet is the focused one
                    // focusedElements" row skipping strategy
                    else if (focusedSheet != null && focusedRow != null) {

//                        // only process the focused row and the first read row (Apache POI skips empty rows when iterating over them)
//                        if(rowNum != focusedRow && rowCount != 0)
                        // array containing index of rows to keep
                        int[] keepRows = new int[]{0, focusedRow-1, focusedRow,  focusedRow+1};
                        // if a row don't have its index in the array, skip it
                        if (Arrays.stream(keepRows).filter(r -> r == rowNum ).count() == 0)
                            continue;
                    }

                    // cells of the current row
                    List<ImportableCell> rowCellsMetadata = Lists.newArrayList();

                    // metadata of the current row
                    ImportableRow rowMetadata = new ImportableRow() {
                        @Override
                        public ImportableSheet getSheet() {
                            return sheetMetadata;
                        }

                        @Override
                        public List<ImportableCell> getCells() {
                            return rowCellsMetadata;
                        }

                        @Override
                        public Integer getRowNum() {
                            return rowNum;
                        }
                    };

                    // iterator on cells in a row
                    Iterator<Cell> cellIterator = row.cellIterator();

                    // for each cell
                    while (cellIterator.hasNext()) {

                        Cell cell = cellIterator.next();
                        final Integer colNum = cell.getColumnIndex();

                        final Object cellValue;
                        switch (cell.getCellType()) {
                            case Cell.CELL_TYPE_NUMERIC:
                                cellValue = cell.getNumericCellValue();
                                break;
                            case Cell.CELL_TYPE_STRING:
                                cellValue = cell.getStringCellValue();
                                break;
                            default:
                                cellValue = null;
                                break;
                        }
                        // adding cell metadata to the list
                        ImportableCell workbookCell = new ImportableCell() {
                            @Override
                            public Integer getIndex() {
                                return cell.getColumnIndex();
                            }

                            @Override
                            public String getValue() {
                                return cellValue == null ? "" : cellValue.toString();
                            }
                        };
                        rowCellsMetadata.add(workbookCell);

                    }

                    // how to process rows and store their data
                    rowConsumer.consumeRow(rowMetadata);

                }
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException("An error occured when trying to parse the provided file", ex);
        }
    }
}
