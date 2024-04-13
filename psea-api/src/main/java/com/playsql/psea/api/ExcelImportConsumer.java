package com.playsql.psea.api;

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
     * @deprecated Use {@link #ExcelImportConsumer(Integer, Integer, String, Integer, List)}
     */
    public ExcelImportConsumer(Integer maxRows, String focusedSheet, Integer focusedRow, List<String> inactiveSheets) {
        this.maxRows = maxRows;
        this.focusedSheet = focusedSheet;
        this.focusedRow = focusedRow;
        this.inactiveSheets = inactiveSheets;
        this.maxRecordsPerTransaction = 1000;
    }

    /** In 1.9.4, changed the second parameter back from int to Integer, because it caused an error on production (and not in development) */
    public ExcelImportConsumer(Integer maxRows, Integer maxRecordsPerTransaction, String focusedSheet, Integer focusedRow, List<String> inactiveSheets) {
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
