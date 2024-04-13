package com.requirementyogi.datacenter.psea.api;

import java.util.List;

public interface Sheet {
    Row addRow(List<? extends Value> values);

    Row addRow(int position, List<? extends Value> values);

    /** Shift all existing rows, from the top
     * @return the number of rows shifted */
    int shiftRows(int count);

    void autoSizeHeaders();

    void setHeightInPoints(int rowNumber, float height);

    /**
     * Freezes the rows
     *
     * @param colSplit      Horizontal position of split.
     * @param rowSplit      Vertical position of split.
     * @param leftmostColumn   Left column visible in right pane.
     * @param topRow        Top row visible in bottom pane
     */
    void freezePanes(int colSplit, int rowSplit, int leftmostColumn, int topRow);

    /**
     * Merges cells
     * @param firstRow the first row, 0-based
     * @param lastRow the first row, 0-based, inclusive
     * @param firstCol the first column, 0-based
     * @param lastCol the last column, 0-based, inclusive
     */
    void addMergedRegion(int firstRow, int lastRow, int firstCol, int lastCol);

    int getRowNum();
    void setRowNum(int rowNum);
}
