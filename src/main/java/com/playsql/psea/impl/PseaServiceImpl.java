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
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.formula.eval.NotImplementedException;
import org.apache.poi.ss.formula.eval.NotImplementedFunctionException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.util.SAXHelper;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.extractor.XSSFEventBasedExcelExtractor;
import org.apache.poi.xssf.model.SharedStringsTable;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
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

    public void extract(InputStream stream, String fileName, ExcelImportConsumer rowConsumer) {
        optimizedExtract(stream, fileName, rowConsumer);
    }

    private void optimizedExtract(InputStream stream, String fileName, ExcelImportConsumer rowConsumer){

        // no data to parse
        if (fileName == null || stream == null)
            return;
        // parse configuration is mandatory
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
        Object inactiveSheetsConfiguration = config.get("inactiveSheets");
        List<String> inactiveSheets = inactiveSheetsConfiguration == null ? Lists.newArrayList() : Lists.newArrayList((String[]) inactiveSheetsConfiguration);
        // try reading inputstream
        try  {

            ImportableWorkbookAPI workbook = () -> fileName;

            // apache poi representation of a .xls excel file
            Workbook excelWorkbook = WorkbookFactory.create(stream);
            // formula eveluator for workbook
            FormulaEvaluator evaluator = excelWorkbook.getCreationHelper().createFormulaEvaluator();
            // clear cached values
            evaluator.clearAllCachedResultValues();
            // ignore cross workbook references (formula referencing external workbook cells)
            evaluator.setIgnoreMissingWorkbooks(true);
            // iterator on sheets
            Iterator<Sheet> sheetIterator = excelWorkbook.sheetIterator();
            List<Sheet> sheets =  Lists.newArrayList();

            // for each sheet
            while (sheetIterator.hasNext()) {

                Sheet sheet = sheetIterator.next();
                String sheetName = sheet.getSheetName();

                // if the current sheet need to be skipped
                if (inactiveSheets.contains(sheetName))
                    continue;

                sheets.add(sheet);
            }

            sheets.stream().forEach(sheet -> {
                String sheetName = sheet.getSheetName();
                final int headerRowNum = sheet.getFirstRowNum();
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

                    @Override
                    public Integer getHeaderRowNum() {
                        // store the num of the header row
                        return headerRowNum;
                    }
                };

                Consumer<Row> internalRowConsumer =  internalRow -> {

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
                            return internalRow.getRowNum();
                        }
                    };

                    // iterator on cells in a row
                    Iterator<Cell> cellIterator = internalRow.cellIterator();

                    // for each cell
                    while (cellIterator.hasNext()) {

                        Cell cell = cellIterator.next();

                        final Object cellValue = computeCellValue(cell, evaluator);

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
                };


                if (maxRows != null && maxRows == 1) {
                    // here we only process one row : only the first one for each sheet
                    internalRowConsumer.accept(sheet.getRow(headerRowNum));
                } else {
                    // here we need to process many rows


                    if(!sheetName.equals(focusedSheet)){
                        // for a normal sheet
                        if(maxRows == null){
                            // if there is no rows to skip  we process them all
                            // iterator on rows in a sheet
                            Iterator<Row> rowIterator = sheet.rowIterator();
                            while (rowIterator.hasNext())
                                // invoking internal row consumer
                                internalRowConsumer.accept(rowIterator.next());

                        } else {
                            // if otherwies we need to skip some rows,  we process "maxRows" including the first one
                            for (int i = 0; i < maxRows ; i++) {
                                internalRowConsumer.accept(sheet.getRow(headerRowNum + i));
                            }
                        }
                    } else if(focusedSheet != null && focusedRow != null){
                        // for a focused sheet
                        final int[] keepRows = new int[]{headerRowNum, focusedRow - 1, focusedRow, focusedRow + 1};
                        // if a row don't have its index in the array, skip it
                        Arrays.stream(keepRows)
                                .distinct()
                                .mapToObj(i -> sheet.getRow(i))
                                .filter(row -> row != null)
                                .forEach( internalRowConsumer::accept);
                    }
                }
            });
        } catch (Exception ex) {
            throw new IllegalArgumentException("An error occured when trying to parse the provided file", ex);
        }
    }


    private void basicExtract(InputStream stream, String fileName, ExcelImportConsumer rowConsumer){

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
        Object inactiveSheetsConfiguration = config.get("inactiveSheets");
        List<String> inactiveSheets = inactiveSheetsConfiguration == null ? Lists.newArrayList() : Lists.newArrayList((String[]) inactiveSheetsConfiguration);
        // try reading inputstream
        try  {

            ImportableWorkbookAPI workbook = () -> fileName;

            // apache poi representation of a .xls excel file
            Workbook excelWorkbook = WorkbookFactory.create(stream);
            // formula eveluator for workbook
            FormulaEvaluator evaluator = excelWorkbook.getCreationHelper().createFormulaEvaluator();
            // clear cached values
            evaluator.clearAllCachedResultValues();
            // ignore cross workbook references (formula referencing external workbook cells)
            evaluator.setIgnoreMissingWorkbooks(true);
            // iterator on sheets
            Iterator<Sheet> sheetIterator = excelWorkbook.sheetIterator();
            List<Sheet> sheets =  Lists.newArrayList();

            // for each sheet
            while (sheetIterator.hasNext()) {

                Sheet sheet = sheetIterator.next();
                String sheetName = sheet.getSheetName();

                // if the current sheet need to be skipped
                if (inactiveSheets.contains(sheetName))
                    continue;

               sheets.add(sheet);
            }
            
            sheets.stream().forEach(sheet -> {
                String sheetName = sheet.getSheetName();
                final int headerRowNum = sheet.getFirstRowNum();
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

                    @Override
                    public Integer getHeaderRowNum() {
                        // store the num of the header row
                        return headerRowNum;
                    }
                };

                Consumer<Row> internalRowConsumer =  internalRow -> {

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
                            return internalRow.getRowNum();
                        }
                    };

                    // iterator on cells in a row
                    Iterator<Cell> cellIterator = internalRow.cellIterator();

                    // for each cell
                    while (cellIterator.hasNext()) {

                        Cell cell = cellIterator.next();

                        final Object cellValue = computeCellValue(cell, evaluator);

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
                };


                if (maxRows != null && maxRows == 1) {
                    // here we only process one row : only the first one for each sheet
                    internalRowConsumer.accept(sheet.getRow(headerRowNum));
                } else {

                    // here we need to process many rows

                    // iterator on rows in a sheet
                    Iterator<Row> rowIterator = sheet.rowIterator();

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
                                break;
                            }
                        }
                        // The current sheet is the focused one
                        // focusedElements" row skipping strategy
                        else if (focusedSheet != null && focusedRow != null) {

//                        // only process the focused row and the first read row (Apache POI skips empty rows when iterating over them)
//                        if(rowNum != focusedRow && rowCount != 0)
                            // array containing index of rows to keep
                            int[] keepRows = new int[]{headerRowNum, focusedRow - 1, focusedRow, focusedRow + 1};
                            // if a row don't have its index in the array, skip it
                            if (Arrays.stream(keepRows).filter(r -> r == rowNum).count() == 0)
                                continue;
                        }

                        internalRowConsumer.accept(row);

                    }
                }
            });
        } catch (Exception ex) {
            throw new IllegalArgumentException("An error occured when trying to parse the provided file", ex);
        }
    }

    private Object computeCellValue(Cell cell, FormulaEvaluator evaluator){
        Object cellValue;
        try{
            CellValue computedFormulaValue = evaluator.evaluate(cell) ;
            if(computedFormulaValue != null){
                switch (computedFormulaValue.getCellType()) {
                    case Cell.CELL_TYPE_NUMERIC:
                        cellValue = cell.getNumericCellValue();
                        break;
                    case Cell.CELL_TYPE_STRING:
                        cellValue = cell.getStringCellValue();
                        break;
                    case Cell.CELL_TYPE_BOOLEAN:
                        cellValue = cell.getBooleanCellValue();
                        break;
                    case Cell.CELL_TYPE_BLANK:
                        cellValue = "";
                        break;
                    case Cell.CELL_TYPE_ERROR:
                        cellValue = ((XSSFCell) cell).getErrorCellString();
                        break;
                    default:
                        cellValue = null;
                        break;
                }
            }else{
                cellValue = null;
            }
        } catch(NotImplementedException ex){
            cellValue = cell.getCellFormula();
        }
        return cellValue;
    }


    public XMLReader fetchSheetParser(SharedStringsTable sst) throws SAXException, ParserConfigurationException {
        XMLReader parser = SAXHelper.newXMLReader();
        ContentHandler handler = new SheetHandler(sst);
        parser.setContentHandler(handler);
        return parser;
    }

    /**
     * See org.xml.sax.helpers.DefaultHandler javadocs
     */
    private static class SheetHandler extends DefaultHandler {
        private SharedStringsTable sst;
        private String lastContents;
        private boolean nextIsString;

        private SheetHandler(SharedStringsTable sst) {
            this.sst = sst;
        }

        public void startElement(String uri, String localName, String name,
                                 Attributes attributes) throws SAXException {
            // c => cell
            if(name.equals("c")) {
                // Print the cell reference
                System.out.print(attributes.getValue("r") + " - ");
                // Figure out if the value is an index in the SST
                String cellType = attributes.getValue("t");
                if(cellType != null && cellType.equals("s")) {
                    nextIsString = true;
                } else {
                    nextIsString = false;
                }
            }
            // Clear contents cache
            lastContents = "";
        }

        public void endElement(String uri, String localName, String name)
                throws SAXException {
            // Process the last contents as required.
            // Do now, as characters() may be called more than once
            if(nextIsString) {
                int idx = Integer.parseInt(lastContents);
                lastContents = sst.getEntryAt(idx).toString();
                nextIsString = false;
            }

            // v => contents of a cell
            // Output after we've seen the string contents
            if(name.equals("v")) {
                System.out.println(lastContents);
            }
        }

        public void characters(char[] ch, int start, int length) {
            lastContents += new String(ch, start, length);
        }
    }

}
