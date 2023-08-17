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

public abstract class ExcelImportConsumer {

    private Integer maxRows;
    private String focusedSheet;
    private Integer focusedRow;
    private List<String> inactiveSheets;
    private final int maxRecordsPerTransaction;

    /**
     * @deprecated Use {@link #ExcelImportConsumer(Integer, Integer, String, Integer, List)}
     */
    public ExcelImportConsumer(Integer maxRows, String focusedSheet, Integer focusedRow, List<String> inactiveSheets) {
        this.maxRows = maxRows;
        this.focusedSheet = focusedSheet;
        this.focusedRow = focusedRow;
        this.inactiveSheets = inactiveSheets;
        this.maxRecordsPerTransaction = 1000;
    }

    public ExcelImportConsumer(Integer maxRows, Integer maxRecordsPerTransaction, String focusedSheet, Integer focusedRow, List<String> inactiveSheets) {
        this.maxRows = maxRows;
        this.maxRecordsPerTransaction = maxRecordsPerTransaction;
        this.focusedSheet = focusedSheet;
        this.focusedRow = focusedRow;
        this.inactiveSheets = inactiveSheets;
    }

    public abstract void consumeNewSheet(String name, List<String> headerRow);

    /**
     * Whenever PSEA starts a new transaction, this method is called so that the plugin can refresh
     * the "Stashes" or re-read data from the database.
     */
    public void onNewTransaction() {
        // Plugins will refresh the stashes and AO references in this function
    }

    /**
     * Push a row to the consumer.
     *
     * @param isFocused whether the row matches `focusedElements`
     * @param rowNum the row number, counted as in the original file, 1-based. It means rowNum is never 1, because
     *               even if title are stacked at the top of the sheet, the first row is still the titles
     * @param cells the list of cell values
     */
    public abstract void consumeRow(boolean isFocused, int rowNum, List<String> cells);

    /**
     * This method is called when a sheet is done processing. If it is the last sheet,
     * {@link #endOfWorkbook()} is also called after this method.
     * @param sheetName the name of the sheet
     */
    public void endOfSheet(String sheetName) {}

    /**
     * This method is called when a workbook is done processing.
     * @return a value which is returned to the caller
     */
    public Object endOfWorkbook() {
        return null;
    }

    public Integer getMaxRows() {
        return maxRows;
    }

    public String getFocusedSheet() {
        return focusedSheet;
    }

    public Integer getFocusedRow() {
        return focusedRow;
    }

    public int getMaxRecordsPerTransaction() {
        return maxRecordsPerTransaction;
    }

    public boolean isSheetActive(String sheetName) {
        if (inactiveSheets != null) {
            return !inactiveSheets.contains(sheetName);
        }
        return true;
    }

    /**
     * Callback method, so that the caller can know what is the progress with regards
     * to the total size of the spreadsheet.
     *
     * There is no contractual guarantee that it is called regularly, or at all; For example
     * it will not be called at all if there is a focused row; It will not be called
     * for non-active sheets.
     *
     * There is a guarantee that current == total is only true at the end of the workbook.
     *
     * @param current the current number of items processed
     * @param total the total number of items to process, so that current==total at the end.
     */
    public void setProgress(int current, int total) {
        // Plugins can implement this method
    }
}
