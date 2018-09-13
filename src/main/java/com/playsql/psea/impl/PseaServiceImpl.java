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
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playsql.psea.api.PseaService;
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
import java.util.ArrayList;
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

    public JsonElement extract(FileInputStream stream, String fileName){
        // null check
        if (fileName == null || stream == null)
            return null;

        JsonObject workbook = new JsonObject();

        workbook.addProperty("name",fileName);

        workbook.addProperty("integrationState","PENDING");

        // try reading inputstream
        try  {

            // metadata on worksheets
            JsonArray worksheets = new JsonArray();

            // apache poi representation of a .xls excel file
            Workbook excelWorkbook = WorkbookFactory.create(stream);
            // iterator on sheets
            Iterator<Sheet> sheetIterator = excelWorkbook.sheetIterator();
            // for each sheet
            while (sheetIterator.hasNext()) {

                Sheet sheet = sheetIterator.next();

                // metadata of the current sheet
                JsonObject sheetMetadata = new JsonObject();

                // rows of the current sheet
                JsonArray sheetRowsMetadata = new JsonArray();

                // integration configuration of the current sheet
                JsonObject sheetIntegrationConfigMetadata = new JsonObject();

                // grouping unit of data present on a sheet, all requirements on the sheet will have this property set
                sheetIntegrationConfigMetadata.addProperty("category", "");

                // integration configuration maps sheet columns to requirement properties
                JsonArray columnsMapping = new JsonArray();

                sheetIntegrationConfigMetadata.add("columnsMapping", columnsMapping);

                // iterator on rows in a sheet
                Iterator<Row> rowIterator = sheet.rowIterator();

                int rowNum = -1;
                // for each row
                while (rowIterator.hasNext()) {

                    Row row = rowIterator.next();
                    rowNum = row.getRowNum();

                    // metadata of the current row
                    JsonObject rowMetadata = new JsonObject();

                    // cells of the current row
                    JsonArray rowCellsMetadata = new JsonArray();


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
                            JsonObject columnMapping = new JsonObject();
                            columnMapping.addProperty("index",colNum);
                            columnMapping.addProperty("mapping","");
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
                        JsonObject workbookCell = new JsonObject();
                        workbookCell.addProperty("index", colNum);
                        workbookCell.addProperty("value", cellValue == null ? "" : cellValue.toString());
                        rowCellsMetadata.add(workbookCell);

                    }

                    //store the row number
                    rowMetadata.addProperty("rowNum", rowNum);

                    //store the cells
                    rowMetadata.add("cells",rowCellsMetadata);

                    //adding row Metadata to list
                    sheetRowsMetadata.add(rowMetadata);
                }

                // store the name of the current sheet
                sheetMetadata.addProperty("name", sheet.getSheetName());
                // store the rows
                sheetMetadata.add("rows", sheetRowsMetadata);
                // store the integration configuration
                sheetMetadata.add("integrationConfig", sheetIntegrationConfigMetadata);

                //adding sheet Metadata to list
                worksheets.add(sheetMetadata);
            }
            workbook.add("worksheets", worksheets);


        } catch (Exception ex) {
            throw new IllegalArgumentException("An error occured when trying to parse the provided file", ex);
        }

        return workbook;
    }


    public com.playsql.psea.api.Workbook extract2(FileInputStream stream, String fileName, Map<String, Object> rowConsumptionInOut, Consumer<com.playsql.psea.api.Workbook.Row> rowConsumer){

        // null check
        if (fileName == null || stream == null)
            return null;

        com.playsql.psea.api.Workbook workbook = new  com.playsql.psea.api.Workbook();

        workbook.setName(fileName);

        workbook.setIntegrationState(IntegrationState.PENDING);

        // try reading inputstream
        try  {

            // metadata on worksheets
//            JsonArray worksheets = new JsonArray();
            List<Worksheet> worksheets = Lists.newArrayList();
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

                // rows of the current sheet
//                JsonArray sheetRowsMetadata = null;
                List<com.playsql.psea.api.Workbook.Row> sheetRowsMetadata = null;

                // max rows to be processed
                Integer maxRows = null;

                // TODO pprocessing data from outside
                // buffer object to store rows temporarily
                if(rowConsumptionInOut != null){
                    Object rowContainer = rowConsumptionInOut.get("sheetRowsMetadata");
                    if(rowContainer != null) {
//                        sheetRowsMetadata = (JsonArray)rowContainer;
                        sheetRowsMetadata = (ArrayList<com.playsql.psea.api.Workbook.Row>) rowContainer;
                        sheetRowsMetadata.clear();
                    }
                    Object maxRowsObject = rowConsumptionInOut.get("max");
                    if(maxRowsObject != null){
                        maxRows = (Integer)maxRowsObject;
                    }

                }



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
                    // skippingall rows with  index is greater maxRows
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
                // store the rows
                if(sheetRowsMetadata!=null)
//                    sheetMetadata.add("rows", sheetRowsMetadata);
                    sheetMetadata.getRows().addAll(sheetRowsMetadata);

                // store the integration configuration
//                sheetMetadata.add("integrationConfig", sheetIntegrationConfigMetadata);
                sheetMetadata.setIntegrationConfig(sheetIntegrationConfigMetadata);

                //adding sheet Metadata to list
                worksheets.add(sheetMetadata);
            }
//            workbook.add("worksheets", worksheets);
            workbook.setWorksheets(worksheets);


        } catch (Exception ex) {
            throw new IllegalArgumentException("An error occured when trying to parse the provided file", ex);
        }

        return workbook;
    }
}
