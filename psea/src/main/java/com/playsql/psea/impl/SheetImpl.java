package com.playsql.psea.impl;

import com.playsql.psea.api.Row;
import com.playsql.psea.api.Sheet;
import com.playsql.psea.api.Value;
import com.playsql.psea.dto.PseaLimitException;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.SheetUtil;
import org.apache.poi.xssf.usermodel.XSSFCreationHelper;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import java.util.List;

public class SheetImpl implements Sheet {
    private final WorkbookAPIImpl workbook;
    private final XSSFCreationHelper helper;
    private final XSSFSheet sheet;
    private int rowNum = 1;
    private final static int CHARACTER_WIDTH = 256;
    private final static int MAX_COLUMN_WIDTH = 255;
    private final static int MIN_COLUMN_WIDTH = 3500; // About 12 characters, 256 units wide

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
        workbook.checkTimer();
        if (position > workbook.getRowLimit()) {
            throw new PseaLimitException(position, workbook.getRowLimit(), "rows", "rows");
        }

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

    /** Adds the size of this cell to the total size of the file, and throw an exception if the Excel file is too big. */
    void addSize(Value value) {
        int size = value == null ? 0 : 10 // We estimate that even an empty cell takes 10 bytes to save.
                + (value.getValue() != null ? value.getValue().length() : 0)
                + (value.getHref() != null ? value.getHref().length() : 0);
        if (size > 0)
            workbook.addSize(size);
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
                XSSFRow row = sheet.getRow(i);
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
        XSSFRow row = sheet.getRow(rowNumber);
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

    XSSFCreationHelper getHelper() {
        return helper;
    }
}
