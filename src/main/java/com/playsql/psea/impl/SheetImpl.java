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

import com.playsql.psea.api.Row;
import com.playsql.psea.api.Sheet;
import com.playsql.psea.api.Value;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCreationHelper;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import java.util.List;

public class SheetImpl implements Sheet {
    private final WorkbookAPIImpl workbook;
    private final XSSFCreationHelper helper;
    private final XSSFSheet sheet;
    private int rowNum = 1;

    public SheetImpl(WorkbookAPIImpl workbook, XSSFSheet sheet) {
        this.workbook = workbook;
        this.helper = workbook.getWorkbook().getCreationHelper();
        this.sheet = sheet;
    }

    @Override
    public Row addRow(List<? extends Value> values) {
        return addRow(rowNum++, values);
    }
    @Override
    public Row addRow(int position, List<? extends Value> values) {
        XSSFRow xlRow = sheet.createRow(position);
        Row row = new RowImpl(this, xlRow);
        for (int col = 0 ; col < values.size() ; col++) {
            Value value = values.get(col);
            if (value != null && value.getValue() != null) {
                row.setCell(col, value);
            }
        }
        return row;
    }

    @Override
    public void autoSizeHeaders() {
        XSSFRow row0 = sheet.getRow(0);
        if (row0 != null) {
            int col = 0;

            XSSFCell cell;
            while (col < 3000 && null != (cell = row0.getCell(col))) {
                String rawValue = cell.getRawValue();
                if (rawValue != null && !rawValue.equals("")) {
                    sheet.autoSizeColumn(col);
                    /*final int CHARACTER_WIDTH = 256;
                    final int MAX_COLUMN_WIDTH = 255 * CHARACTER_WIDTH;
                    sheet.setColumnWidth(col, Math.min(sheet.getColumnWidth(col) + CHARACTER_WIDTH * 3, MAX_COLUMN_WIDTH));*/
                }
                col++;
            }
        }
    }

    @Override
    public void setHeightInPoints(int rowNumber, float height) {
        XSSFRow row = sheet.getRow(rowNumber);
        if (row != null) {
            row.setHeightInPoints(height);
        }
    }

    @Override
    public void freezePanes(int colSplit, int rowSplit, int leftmostColumn, int topRow) {
        sheet.createFreezePane(colSplit, rowSplit, leftmostColumn, topRow);
    }

    @Override
    public void addMergedRegion(int firstRow, int lastRow, int firstCol, int lastCol) {
        sheet.addMergedRegion(new CellRangeAddress(firstRow, lastRow, firstCol, lastCol));
    }

    public int getRowNum() {
        return rowNum;
    }

    public void setRowNum(int rowNum) {
        this.rowNum = rowNum;
    }

    WorkbookAPIImpl getWorkbook() {
        return workbook;
    }

    XSSFCreationHelper getHelper() {
        return helper;
    }
}
