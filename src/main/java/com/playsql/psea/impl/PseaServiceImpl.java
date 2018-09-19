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

        // null check
        if (fileName == null || stream == null)
            return;

//        com.playsql.psea.api.Workbook workbook = new  com.playsql.psea.api.Workbook();

        com.playsql.psea.api.Workbook workbook = rowConsumer.getOutput();

        workbook.setName(fileName);

        workbook.setIntegrationState(IntegrationState.PENDING);

        // try reading inputstream
        try  {

            // metadata on worksheets
//            JsonArray worksheets = new JsonArray();
            List<Worksheet> worksheets = Lists.newArrayList();

            //            workbook.add("worksheets", worksheets);
            workbook.setWorksheets(worksheets);
            // apache poi representation of a .xls excel file
            Workbook excelWorkbook = WorkbookFactory.create(stream);
            // iterator on sheets
            Iterator<Sheet> sheetIterator = excelWorkbook.sheetIterator();
            // for each sheet
            while (sheetIterator.hasNext()) {

                Sheet sheet = sheetIterator.next();

                // metadata of the current sheet
//                JsonObject sheetMetadata = new JsonObject();
                com.playsql.psea.api.Workbook.Worksheet sheetMetadata = new Worksheet();

                //adding sheet Metadata to list
                worksheets.add(sheetMetadata);

                // max rows to be processed
                Integer maxRows = null;

                // TODO pprocessing data from outside
                // buffer object to store rows temporarily
                Map<String, Object> rowConsumptionInOut = rowConsumer.getRowConsumptionInOut();
                if(rowConsumptionInOut != null){

                    Object maxRowsObject = rowConsumptionInOut.get("max");
                    if(maxRowsObject != null){
                        maxRows = (Integer)maxRowsObject;
                    }

                }

                //
                // integration configuration of the current sheet
//                JsonObject sheetIntegrationConfigMetadata = new JsonObject();
                IntegrationConfig sheetIntegrationConfigMetadata = new IntegrationConfig();

                // grouping unit of data present on a sheet, all requirements on the sheet will have this property set
//                sheetIntegrationConfigMetadata.addProperty("category", "");
                sheetIntegrationConfigMetadata.setCategory("");

                // integration configuration maps sheet columns to requirement properties
//                JsonArray columnsMapping = new JsonArray();
                List<ColumnMapping> columnsMapping =  Lists.newArrayList();

//                sheetIntegrationConfigMetadata.add("columnsMapping", columnsMapping);
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
                            ColumnMapping columnMapping = new ColumnMapping(colNum, "");

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
                       /* JsonObject workbookCell = new JsonObject();
                        workbookCell.addProperty("index", colNum);
                        workbookCell.addProperty("value", cellValue == null ? "" : cellValue.toString());*/
                        com.playsql.psea.api.Workbook.Cell workbookCell = new com.playsql.psea.api.Workbook.Cell(colNum,cellValue == null ? "" : cellValue.toString());
                        rowCellsMetadata.add(workbookCell);

                    }

                    //store the row number
//                    rowMetadata.addProperty("rowNum", rowNum);
                    rowMetadata.setRowNum(rowNum);

                    //store the cells
//                    rowMetadata.add("cells", rowCellsMetadata);
                    rowMetadata.setCells(rowCellsMetadata);

                    // how to process rows and store their data
                    rowConsumer.accept(rowMetadata);

                }

                // store the name of the current sheet
//                sheetMetadata.addProperty("name", sheet.getSheetName());
                sheetMetadata.setName(sheet.getSheetName());

                // store the integration configuration
//                sheetMetadata.add("integrationConfig", sheetIntegrationConfigMetadata);
                sheetMetadata.setIntegrationConfig(sheetIntegrationConfigMetadata);

            }



        } catch (Exception ex) {
            throw new IllegalArgumentException("An error occured when trying to parse the provided file", ex);
        }
    }
}
