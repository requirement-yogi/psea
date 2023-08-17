package com.playsql.psea.api;

/*
 * #%L
 * Play SQL Exports
 * %%
 * Copyright (C) 2016 - 2023 Requirement Yogi S.A.S.U.
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
