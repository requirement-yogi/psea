package com.playsql.psea.api;

/*
 * #%L
 * Play SQL Exports
 * %%
 * Copyright (C) 2016 - 2024 Requirement Yogi S.A.S.U.
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

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

public abstract class ExcelImportConsumer {

    private Integer maxRows;
    private String focusedSheet;
    private Integer focusedRow;
    private List<String> inactiveSheets;
    private final int maxRecordsPerTransaction;

    /**
     * @deprecated Use {@link #ExcelImportConsumer(Integer, int, String, Integer, List)}
     */
    public ExcelImportConsumer(Integer maxRows, String focusedSheet, Integer focusedRow, List<String> inactiveSheets) {
        this.maxRows = maxRows;
        this.focusedSheet = focusedSheet;
        this.focusedRow = focusedRow;
        this.inactiveSheets = inactiveSheets;
        this.maxRecordsPerTransaction = 1000;
    }

    public ExcelImportConsumer(Integer maxRows, int maxRecordsPerTransaction, String focusedSheet, Integer focusedRow, List<String> inactiveSheets) {
        this.maxRows = maxRows;
        this.maxRecordsPerTransaction = maxRecordsPerTransaction;
        this.focusedSheet = focusedSheet;
        this.focusedRow = focusedRow;
        this.inactiveSheets = inactiveSheets;
    }

    /**
     * @deprecated since 1.9, call {@link #consumeNewSheet(ImportableSheet)}
     */
    public void consumeNewSheet(String name, List<String> headerRow) {
        // Consumers can implement this method
    }

    /**
     * Called when a new sheet starts
     *
     * @param sheet the details of the sheet
     *
     * @since 1.9
     */
    public void consumeNewSheet(ImportableSheet sheet) {
        ImportableRow headerRow = sheet.getHeaderRow();
        consumeNewSheet(sheet.getName(),
                        headerRow != null ? headerRow.getCellsAsString(true) : null
        );
    };

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
     * @deprecated since 1.9, see {@link #consumeImportableRow}
     */
    public void consumeRow(boolean isFocused, int rowNum, @Nullable List<String> cells) {
        // Consumers should implement this method
    }

    /**
     * Push a row to the consumer. Same as {@link #consumeRow}, but with ImportableCells.
     *
     * @since 1.9
     */
    public void consumeImportableRow(ImportableRow row) {
        consumeRow(row.isFocused(), row.getRowNum(), row.getCellsAsString(true));
    }

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
