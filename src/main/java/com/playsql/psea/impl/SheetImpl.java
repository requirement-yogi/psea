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

import com.playsql.psea.api.Row;
import com.playsql.psea.api.Sheet;
import com.playsql.psea.api.Value;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.SheetUtil;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;

import java.util.List;

public class SheetImpl implements Sheet {
    private final WorkbookAPIImpl workbook;
    private final CreationHelper helper;
    private final SXSSFSheet sheet;
    private int rowNum = 1;
    private final static int CHARACTER_WIDTH = 256;
    private final static int MAX_COLUMN_WIDTH = 255;
    private final static int MIN_COLUMN_WIDTH = 3500; // About 12 characters, 256 units wide

    public SheetImpl(WorkbookAPIImpl workbook, SXSSFSheet sheet) {
        this.workbook = workbook;
        this.helper = workbook.getWorkbook().getCreationHelper();
        this.sheet = sheet;
        this.sheet.trackAllColumnsForAutoSizing();
    }

    @Override
    public Row addRow(List<? extends Value> values) {
        return addRow(rowNum++, values);
    }

    @Override
    public Row addRow(int position, List<? extends Value> values) {
        workbook.checkTimer();
        if (workbook.getRowLimit() != null && position > workbook.getRowLimit()) {
            throw new IllegalArgumentException(
                    "Row number (" + position + ") is outside the configured range (0.." + workbook.getRowLimit() + ")");
        }

        SXSSFRow xlRow = sheet.createRow(position);
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
    public int shiftRows(int count) {
        sheet.shiftRows(0, sheet.getLastRowNum(), count);
        return count;
    }

    @Override
    public void autoSizeHeaders() {
        int col = 0;
        int countEmptyColumns = 0;

        while (col < 3000) {
            // First, check there are values in the first 5 rows
            boolean isEmpty = true;
            for (int i = sheet.getFirstRowNum() ; i < sheet.getFirstRowNum() + 5 ; i++) {
                SXSSFRow row = sheet.getRow(i);
                if (row == null || row.getCell(col) != null) {
                    isEmpty = false;
                    break;
                }
            }
            if (isEmpty) {
                countEmptyColumns++;
                if (countEmptyColumns > 3) return; // We stop autosizing after 3 empty columns
                continue; // We don't autosize if there is no value in the first 5 rows.
            }

            // Ok, autosize this column:
            double widthInChars = SheetUtil.getColumnWidth(sheet, col, false);

            if (widthInChars != -1) {
                int widthInChars2 = Math.min((int) widthInChars, MAX_COLUMN_WIDTH);
                if (widthInChars2 > 20) {
                    // Above 20, characters count half
                    widthInChars2 = 20 + (widthInChars2 - 20) / 2;
                }
                int widthIn256th = widthInChars2 * CHARACTER_WIDTH;
                int widthIn256thWithMinimum = Math.max(widthIn256th, MIN_COLUMN_WIDTH);
                sheet.setColumnWidth(col, widthIn256thWithMinimum);
            }

            sheet.autoSizeColumn(col); // TODO Check
            // sheet.autoSizeColumn(col);
            /*final int CHARACTER_WIDTH = 256;
            final int MAX_COLUMN_WIDTH = 255 * CHARACTER_WIDTH;
            sheet.setColumnWidth(col, Math.min(sheet.getColumnWidth(col) + CHARACTER_WIDTH * 3, MAX_COLUMN_WIDTH));*/
            col++;
        }
    }

    @Override
    public void setHeightInPoints(int rowNumber, float height) {
        SXSSFRow row = sheet.getRow(rowNumber);
        if (row != null) {
            row.setHeightInPoints(height);
        }
    }

    @Override
    public void freezePanes(int colSplit, int rowSplit, int leftmostColumn, int topRow) {
        sheet.createFreezePane(colSplit, rowSplit, leftmostColumn, topRow);
    }

    /**
     * Merges cells
     * @param firstRow the first row, 0-based
     * @param lastRow the first row, 0-based, inclusive
     * @param firstCol the first column, 0-based
     * @param lastCol the last column, 0-based, inclusive
     */
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

    CreationHelper getHelper() {
        return helper;
    }
}
