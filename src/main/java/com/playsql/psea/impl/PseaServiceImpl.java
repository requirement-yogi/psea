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
import com.playsql.psea.api.PseaService;
import com.playsql.psea.api.ExcelImportConsumer;
import com.playsql.psea.api.WorkbookAPI;
import com.playsql.psea.api.Workbook.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class PseaServiceImpl implements PseaService {

    @Override
    public String getVersion() {
        return "1.2";
    }

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

    public void extract2(FileInputStream stream, String fileName, ExcelImportConsumer rowConsumer){

        // no data to parse
        if (fileName == null || stream == null)
            return;

        com.playsql.psea.api.Workbook workbook = rowConsumer.getOptionalOutput();


        // try reading inputstream
        try  {

            // TODO implement processing when optional output is not ptovided
//            if (workbook == null){
//                return;
//            }

            workbook.setName(fileName);

            workbook.setIntegrationState(IntegrationState.PENDING);

            // metadata on worksheets
            List<Worksheet> worksheets = Lists.newArrayList();

            workbook.setWorksheets(worksheets);
            // apache poi representation of a .xls excel file
            Workbook excelWorkbook = WorkbookFactory.create(stream);
            // iterator on sheets
            Iterator<Sheet> sheetIterator = excelWorkbook.sheetIterator();
            // for each sheet
            while (sheetIterator.hasNext()) {

                Sheet sheet = sheetIterator.next();

                // metadata of the current sheet
                com.playsql.psea.api.Workbook.Worksheet sheetMetadata = new Worksheet();

                // store the name of the current sheet
                sheetMetadata.setName(sheet.getSheetName());


                // store the integration configuration
                sheetMetadata.setIntegrationConfig(new IntegrationConfig());


                //adding sheet Metadata to list
                worksheets.add(sheetMetadata);

                // max rows to be processed
                Integer maxRows = null;

                // TODO processing data from outside
                // buffer object to store rows temporarily
                Map<String, Object> rowConsumptionInOut = rowConsumer.getRowConsumptionInOut();
                if(rowConsumptionInOut != null){

                    Object maxRowsObject = rowConsumptionInOut.get("max");
                    if(maxRowsObject != null){
                        maxRows = (Integer)maxRowsObject;
                    }

                }

                // integration configuration of the current sheet
                IntegrationConfig sheetIntegrationConfigMetadata = sheetMetadata.getIntegrationConfig();

                // grouping unit of data present on a sheet, all requirements on the sheet will have this property set
                sheetIntegrationConfigMetadata.setCategory("");

                // integration configuration maps sheet columns to requirement properties
                List<ColumnMapping> columnsMapping =  Lists.newArrayList();

                sheetIntegrationConfigMetadata.setColumnsMapping(columnsMapping);

                // iterator on rows in a sheet
                Iterator<Row> rowIterator = sheet.rowIterator();

                int rowNum = -1;


                // for each row
                while (rowIterator.hasNext()) {

                    Row row = rowIterator.next();
                    rowNum = row.getRowNum();

                    // TODO
                    // if the current row need to be skipped
                    if(maxRows != null && rowNum >= maxRows){
                    // skipping all rows with  index is greater maxRows
                        rowIterator.forEachRemaining(skippedRow -> {});
                        continue;
                    }


                    // metadata of the current row
//                    JsonObject rowMetadata = new JsonObject();
                    com.playsql.psea.api.Workbook.Row rowMetadata = new com.playsql.psea.api.Workbook.Row();
                    //
                    rowMetadata.setSheet(sheetMetadata);

                    // cells of the current row
//                    JsonArray rowCellsMetadata = new JsonArray();
                    List<com.playsql.psea.api.Workbook.Cell> rowCellsMetadata = Lists.newArrayList();


                    // iterator on cells in a row
                    Iterator<Cell> cellIterator = row.cellIterator();
                    // loop on cells
                    int colNum = -1;
                    // for each cell
                    while (cellIterator.hasNext()) {

                        Cell cell = cellIterator.next();
                        colNum = cell.getColumnIndex();

                        // if on the first row
                        if (rowNum == 0) {
                           /* JsonObject columnMapping = new JsonObject();
                            columnMapping.addProperty("index",colNum);
                            columnMapping.addProperty("mapping","");*/
                            ColumnMapping columnMapping = new ColumnMapping(colNum, "", "");

                            columnsMapping.add(columnMapping);
                        }

                        Object cellValue = null;
                        switch (cell.getCellType()) {
                            case Cell.CELL_TYPE_NUMERIC:
                                cellValue = cell.getNumericCellValue();
                                break;
                            case Cell.CELL_TYPE_STRING:
                                cellValue = cell.getStringCellValue();
                                break;
                        }
                        //adding cell Metadata to list
                        com.playsql.psea.api.Workbook.Cell workbookCell = new com.playsql.psea.api.Workbook.Cell(colNum,cellValue == null ? "" : cellValue.toString());
                        rowCellsMetadata.add(workbookCell);

                    }

                    //store the row number
                    rowMetadata.setRowNum(rowNum);

                    //store the cells
                    rowMetadata.setCells(rowCellsMetadata);

                    // how to process rows and store their data
                    rowConsumer.consumeRow(rowMetadata);

                }
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException("An error occured when trying to parse the provided file", ex);
        }
    }
}
