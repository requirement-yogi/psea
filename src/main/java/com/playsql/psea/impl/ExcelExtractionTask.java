package com.playsql.psea.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.google.common.collect.Lists;
import com.playsql.psea.api.ExcelImportConsumer;
import com.playsql.psea.api.ImportableCell;
import com.playsql.psea.api.PSEAImportException;
import com.playsql.psea.api.PseaService;
import com.playsql.psea.impl.beans.ImportableCellImpl;
import com.playsql.psea.impl.beans.ImportableRowImpl;
import com.playsql.psea.impl.beans.ImportableSheetImpl;
import com.playsql.psea.utils.Utils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Implementation of the reading of an Excel file, for imports
 */
@NotThreadSafe
public class ExcelExtractionTask {
    private final static Logger LOG = LoggerFactory.getLogger(ExcelExtractionTask.class);

    private final ExcelImportConsumer rowConsumer;
    private final ActiveObjects ao;

    // 3 ways to skip parts of the spreadsheet
    private final Integer maxRows;
    private final String focusedSheet;
    private final Integer focusedRow;


    // Stateful fields
    private final int maxRecordsPerTransaction;
    private Utils.Clock clock;
    private Workbook workbook;
    private FormulaEvaluator evaluator;
    private int totalItems;

    public ExcelExtractionTask(ActiveObjects ao, ExcelImportConsumer rowConsumer) {
        this.ao = ao;
        this.maxRows = rowConsumer.getMaxRows();
        this.focusedSheet = rowConsumer.getFocusedSheet();
        this.focusedRow = rowConsumer.getFocusedRow();
        this.maxRecordsPerTransaction = rowConsumer.getMaxRecordsPerTransaction();
        this.rowConsumer = rowConsumer;
    }

    private void initialize(PseaService.PseaInput workbookFile) throws OutOfMemoryError, PSEAImportException, IOException {

        clock = Utils.Clock.start("Reading Excel file " + workbookFile.getFileName() + " - ");

        if (workbookFile instanceof PseaService.PseaFileInput) {
            workbook = WorkbookFactory.create(((PseaService.PseaFileInput) workbookFile).getFile());
        } else if (workbookFile instanceof PseaService.PseaInputStream) {
            workbook = WorkbookFactory.create(((PseaService.PseaInputStream) workbookFile).getInputStream());
        } else {
            throw new RuntimeException("Unknown PseaInput class: " + workbookFile);
        }

        // apache poi representation of a .xls excel file
        // formula evaluator for workbook
        evaluator = workbook.getCreationHelper().createFormulaEvaluator();
        // clear cached values
        evaluator.clearAllCachedResultValues();
        // ignore cross workbook references (formula referencing external workbook cells)
        evaluator.setIgnoreMissingWorkbooks(true);

        // Count the total number of items
        totalItems = maxRows != null ? maxRows : getTotalItems(rowConsumer::isSheetActive, workbook);
    }

    public void extract(PseaService.PseaInput workbookFile) throws OutOfMemoryError, PSEAImportException {

        try {
            initialize(workbookFile);
            // iterator on sheets
            Iterator<Sheet> sheetIterator = workbook.sheetIterator();

            LOG.debug(clock.time("Done reading the file"));

            MutableInt processedItems = new MutableInt(0);
            while (sheetIterator.hasNext()) {
                processedItems.increment();
                Sheet sheet = sheetIterator.next();
                String sheetName = sheet.getSheetName();
                if (!rowConsumer.isSheetActive(sheetName)) {
                    continue;
                }
                if (focusedSheet != null && !sheetName.equals(focusedSheet)) {
                    // We push the sheet without providing the cells, because we want the 'excel tab' to display, but only
                    // the focused one to display with cells
                    ao.executeInTransaction(() -> {
                        rowConsumer.consumeNewSheet(new ImportableSheetImpl(sheetName, null));
                        return null;
                    });
                    continue;
                }

                extractRows(
                        processedItems.intValue(),
                        processedItems::add,
                        sheet
                );
                LOG.debug(clock.time("Done reading one sheet"));
            }
            rowConsumer.endOfWorkbook();
            rowConsumer.setProgress(totalItems, totalItems);
        } catch (IOException e) {
            throw new PSEAImportException(workbookFile, e);
        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException e) {
                    LOG.error("Error while closing the Excel file that we were reading", e);
                }
            }
        }
    }

    /**
     * Extract the rows of the sheet
     *
     * @param processedItemsAtStartOfSheet the current item counter
     * @param addProcessedItems a callback to add items that were processed in this call
     * @param sheet the sheet
     */
    private void extractRows(
            int processedItemsAtStartOfSheet,
            Consumer<Integer> addProcessedItems,
            Sheet sheet
    ) {
        int headerRowNum = sheet.getFirstRowNum();
        // If the focusedSheet is not specified, then we process at least the headerRow.
        Row row = sheet.getRow(headerRowNum);
        // If there is no first row, then there is no data on this sheet. Skip it.
        if (row != null) {
            short firstCellNum = row.getFirstCellNum();
            short lastCellNum = row.getLastCellNum();
            List<ImportableCell> headers = readRow(sheet, row, firstCellNum, lastCellNum, evaluator);
            if (headers == null) {
                throw new RuntimeException("The header row is empty for sheet '" + sheet.getSheetName() + "'");
            }
            ao.executeInTransaction(() -> {
                rowConsumer.consumeNewSheet(new ImportableSheetImpl(sheet.getSheetName(), new ImportableRowImpl(
                        headerRowNum, false, headers
                )));
                return null;
            });

            int start = headerRowNum + 1;
            if (focusedRow != null) {
                // focusedRow is 1-based, because it is extracted from rowNum, whereas 'start' or 'i' is 0-based
                // And we want the row before focusedRow
                start = Math.max(focusedRow - 2, start);
            }
            int end = maxRows != null ? Math.min(start + maxRows - 1, sheet.getLastRowNum()) : sheet.getLastRowNum();

            forLoop(
                    start,
                    end,
                    maxRecordsPerTransaction,
                    rowConsumer::onNewTransaction,
                    itemsProcessedInLoop -> rowConsumer.setProgress(Math.min(processedItemsAtStartOfSheet + itemsProcessedInLoop,
                                                                             totalItems - 1),
                                                                    totalItems),
                    i -> {
                        int rowNum = i + 1;
                        boolean isFocused = focusedRow != null && focusedRow == rowNum;
                        rowConsumer.consumeImportableRow(
                                new ImportableRowImpl(
                                        rowNum,
                                        isFocused,
                                        readRow(sheet, sheet.getRow(i), firstCellNum, lastCellNum, evaluator)
                                )
                        );
                    }
            );

            addProcessedItems.accept(end - start);

            ao.executeInTransaction(() -> {
                rowConsumer.endOfSheet(sheet.getSheetName());
                return null;
            });
        }
    }

    private static int getTotalItems(Function<String, Boolean> isSheetActive, Workbook workbook) {
        Iterator<Sheet> sheetIterator1 = workbook.sheetIterator();
        int totalItems = 0;
        while (sheetIterator1.hasNext()) {
            totalItems++; // One for each sheet
            Sheet sheet = sheetIterator1.next();
            String sheetName = sheet.getSheetName();
            if (!isSheetActive.apply(sheetName)) {
                continue;
            }
            totalItems += sheet.getLastRowNum() - sheet.getFirstRowNum(); // One for each row
        }
        totalItems++; // One for the end
        return totalItems;
    }

    /**
     * Perform a 'for' loop, going from 'start' (inclusive) to 'end' (INCLUSIVE TOO),
     * but starts a transaction up to every maxRecordsPerTransaction
     *
     * @param start the start of the loop (inclusive)
     * @param end the end of the loop (/!\ inclusive too)
     * @param maxRecordsPerTransaction the maximum number of records in each transaction
     * @param onNewTransaction a function to call each time a transaction is started
     * @param setProgress a function to call from time to time, which takes as parameter the current number of items
     *                    treated in the loop
     * @param consumer the body of the loop, to which the 'i' counter of the loop is passed.
     *                 {@code start <= i <= end}.
     */
    void forLoop(
            int start,
            int end,
            int maxRecordsPerTransaction,
            Runnable onNewTransaction,
            Consumer<Integer> setProgress,
            Consumer<Integer> consumer
    ) {
        int i1 = start; // i1 == 1, the loop counter, except it's the one outside the transaction
        while (i1 <= end) {
            int i1AtStartOfLoop = i1;
            i1 = ao.executeInTransaction(() -> {
                onNewTransaction.run();
                int i2 = i1AtStartOfLoop; // i2 == i, inside the transaction
                int rowsInTransaction = 0;
                while (i2 <= end && rowsInTransaction < maxRecordsPerTransaction) {
                    consumer.accept(i2);
                    i2++;
                    rowsInTransaction++;
                }
                return i2;
            });
            setProgress.accept(i1 - start);
        }
    }

    private static List<ImportableCell> readRow(Sheet sheet, Row row, short firstCellNum, short lastCellNum, FormulaEvaluator evaluator) {
        if (row != null) {
            List<ImportableCell> values = Lists.newArrayList();
            for (int i = firstCellNum ; i < lastCellNum ; i++) {
                Cell cell = row.getCell(i);
                Cell topLeftCell = resolveMergedRegion(sheet, cell);
                String value = computeCellValue(cell, evaluator);
                String mergedValue = topLeftCell != null ? computeCellValue(topLeftCell, evaluator) : null;

                values.add(new ImportableCellImpl(i, topLeftCell != null, value, mergedValue));
            }
            return values;
        } else {
            return null;
        }
    }

    /**
     * If there is a merged region related to rowNum/colNum, then return the top-left cell of that region,
     * which contains the value and formattin
     */
    private static Cell resolveMergedRegion(Sheet sheet, Cell cell) {
        if (cell == null) return null;
        for (CellRangeAddress region : sheet.getMergedRegions()) {
            if (region.isInRange(cell)) {
                Row firstRow = sheet.getRow(region.getFirstRow());
                Cell firstCell = firstRow.getCell(region.getFirstColumn());
                return firstCell;
            }
        }
        return null;
    }

    private static String computeCellValue(Cell cell, FormulaEvaluator evaluator){
        if (cell == null) return null;
        Object cellValue;
        try {
            CellValue computedFormulaValue = evaluator.evaluate(cell) ;
            if (computedFormulaValue != null){
                switch (computedFormulaValue.getCellType()) {
                    case NUMERIC:
                        cellValue = computedFormulaValue.getNumberValue();
                        break;
                    case STRING:
                        // TODO We could get the RTF to get bolds and similar, but it throws an exception if it's not text
                        // RichTextString richValue = cell.getRichStringCellValue();
                        cellValue = computedFormulaValue.getStringValue();
                        break;
                    case BOOLEAN:
                        cellValue = computedFormulaValue.getBooleanValue();
                        break;
                    case BLANK:
                        cellValue = "";
                        break;
                    case ERROR:
                        cellValue = computedFormulaValue.formatAsString();
                        break;
                    default:
                        cellValue = null;
                        break;
                }
            } else {
                cellValue = null;
            }
        } catch (RuntimeException ex) {
            cellValue = cell.getCellFormula();
        }
        return cellValue == null ? null : Objects.toString(cellValue);
    }

}
