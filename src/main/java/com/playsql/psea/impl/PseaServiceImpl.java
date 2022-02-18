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

import com.atlassian.confluence.api.model.accessmode.AccessMode;
import com.atlassian.confluence.api.service.accessmode.AccessModeService;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.google.common.collect.Lists;
import com.playsql.psea.api.ExcelImportConsumer;
import com.playsql.psea.api.PSEAImportException;
import com.playsql.psea.api.PseaService;
import com.playsql.psea.api.WorkbookAPI;
import com.playsql.psea.db.dao.PseaTaskDAO;
import com.playsql.psea.db.entities.DBPseaTask;
import com.playsql.psea.dto.DTOPseaTask;
import com.playsql.psea.dto.PseaLimitException;
import com.playsql.psea.utils.Utils.Clock;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.playsql.psea.db.entities.DBPseaTask.STATUS_DONE;
import static com.playsql.psea.db.entities.DBPseaTask.STATUS_ERROR;

public class PseaServiceImpl implements PseaService {

    private final static Logger LOG = LoggerFactory.getLogger(PseaServiceImpl.class);
    private static final String FILE_PREFIX = "excel-export-";
    private static final String FILE_EXTENSION = ".xlsx";
    private static final String SETTINGS_ROW_LIMIT = "com.requirementyogi.psea.row-limit";
    private static final String SETTINGS_TIME_LIMIT = "com.requirementyogi.psea.time-limit";
    private static final String SETTINGS_DATA_LIMIT = "com.requirementyogi.psea.data-limit";
    public static final long MAX_ROWS_DEFAULT = 1000000;
    public static final long TIME_LIMIT_DEFAULT = TimeUnit.MINUTES.toMillis(2);
    public static final long TIME_LIMIT_MAX = TimeUnit.HOURS.toMillis(2);
    public static final long DATA_LIMIT_DEFAULT = 1000000;
    public static final long DATA_LIMIT_MAX = 100 * DATA_LIMIT_DEFAULT;

    private final PluginSettingsFactory pluginSettingsFactory;
    private final AccessModeService accessModeService;
    private final PseaTaskDAO dao;

    public PseaServiceImpl(PluginSettingsFactory pluginSettingsFactory,
                           AccessModeService accessModeService,
                           PseaTaskDAO dao
    ) {
        this.pluginSettingsFactory = pluginSettingsFactory;
        this.accessModeService = accessModeService;
        this.dao = dao;
    }

    public File export(Consumer<WorkbookAPI> f) {
        DBPseaTask record = accessModeService.getAccessMode() == AccessMode.READ_WRITE ? dao.create() : null;

        long sizeLimit = getDataLimit();
        Consumer<Long> saveSize = buildSaveSizeFunction(record, sizeLimit);

        XSSFWorkbook xlWorkbook = null;
        long rowLimit = getRowLimit();
        long timeLimit = getTimeLimit();
        File file = null;
        try {
            xlWorkbook = new XSSFWorkbook();
            WorkbookAPIImpl workbook = new WorkbookAPIImpl(xlWorkbook, rowLimit, timeLimit, saveSize);
            f.accept(workbook);

            dao.save(record, DTOPseaTask.Status.WRITING, null);

            file = File.createTempFile(FILE_PREFIX, FILE_EXTENSION);
            file.deleteOnExit();
            if (record != null) {
                record.setFilename(file.getName());
                dao.save(record);
            }

            try (FileOutputStream fileOut = new FileOutputStream(file)) {
                xlWorkbook.write(fileOut);
            }

            dao.save(record, DTOPseaTask.Status.DONE, "Size=" + workbook.getDataSize() + " units");
            return file;

        } catch (IOException e) {
            dao.save(record, DTOPseaTask.Status.ERROR, "IO Exception: " + e.getMessage());
            if (file != null) {
                file.delete(); // Doesn't throw an exception, just returns false if error
            }
            throw new RuntimeException("Error while writing an Excel file to disk", e);

        } catch (RuntimeException re) {
            dao.save(record, DTOPseaTask.Status.ERROR, re.getMessage());
            if (file != null) {
                file.delete(); // Doesn't throw an exception, just returns false if error
            }
            throw re;
        } finally {
            if (record != null) {
                if (!Objects.equals(record.getStatus(), STATUS_ERROR) && !Objects.equals(record.getStatus(), STATUS_DONE)) {
                    dao.save(record, DTOPseaTask.Status.ERROR, "The status was " + record.getStatus() + " despite the export being finished.");
                }
            }
            if (xlWorkbook != null) {
                try {
                    xlWorkbook.close();
                } catch (IOException e) {
                    LOG.error("Error while closing an Excel file that we were creating", e);
                }
            }
        }
    }

    private Consumer<Long> buildSaveSizeFunction(DBPseaTask record, long sizeLimit) {
        Consumer<Long> saveSize = new Consumer<Long>() {

            private final long SIZE_RESOLUTION = 10000; // Save the size every 10KB.
            private long nextSaveSize = 10; // We save just after a few bytes

            @Override
            public void accept(Long currentSize) {
                if (currentSize > sizeLimit) {
                    throw new PseaLimitException(currentSize, sizeLimit, "size", "units");
                }

                if (record != null && currentSize > nextSaveSize) {
                    nextSaveSize = currentSize + SIZE_RESOLUTION;
                    record.setSize(currentSize);
                    dao.save(record);
                }
            }
        };
        return saveSize;
    }

    public void extract(PseaInput workbookFile, ExcelImportConsumer rowConsumer) throws OutOfMemoryError, PSEAImportException {
        Clock clock = Clock.start("Reading Excel file " + workbookFile.getFileName() + " - ");

        // 3 ways to skip parts of the spreadsheet
        Integer maxRows = rowConsumer.getMaxRows();
        String focusedSheet = rowConsumer.getFocusedSheet();
        Integer focusedRow = rowConsumer.getFocusedRow();

        // try reading inputstream
        Workbook workbook = null;
        try {

            if (workbookFile instanceof PseaFileInput) {
                workbook = WorkbookFactory.create(((PseaFileInput) workbookFile).getFile());
            } else if (workbookFile instanceof PseaInputStream) {
                workbook = WorkbookFactory.create(((PseaInputStream) workbookFile).getInputStream());
            } else {
                throw new RuntimeException("Unknown PseaInput class: " + workbookFile);
            }

            // apache poi representation of a .xls excel file
            // formula eveluator for workbook
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            // clear cached values
            evaluator.clearAllCachedResultValues();
            // ignore cross workbook references (formula referencing external workbook cells)
            evaluator.setIgnoreMissingWorkbooks(true);
            // iterator on sheets
            Iterator<Sheet> sheetIterator = workbook.sheetIterator();

            LOG.debug(clock.time("Done reading the file"));

            while (sheetIterator.hasNext()) {
                Sheet sheet = sheetIterator.next();
                String sheetName = sheet.getSheetName();
                final int headerRowNum = sheet.getFirstRowNum();
                if (!rowConsumer.isSheetActive(sheetName)) {
                    continue;
                }
                if (focusedSheet != null && !sheetName.equals(focusedSheet)) {
                    // We push the sheet without providing the cells, because we want the 'excel tab' to display, but only
                    // the focused one to display with cells
                    rowConsumer.consumeNewSheet(sheetName, null);
                    continue;
                }

                // If the focusedSheet is not specified, then we process at least the headerRow.
                Row row = sheet.getRow(headerRowNum);
                // If there is no first row, then there is no data on this sheet. Skip it.
                if (row != null) {
                    short firstCellNum = row.getFirstCellNum();
                    short lastCellNum = row.getLastCellNum();
                    List<String> headers = readRow(row, firstCellNum, lastCellNum, evaluator);
                    if (headers == null) {
                        throw new RuntimeException("The header row is empty for sheet '" + sheetName + "'");
                    }
                    rowConsumer.consumeNewSheet(sheetName, headers);

                    // here we need to process many rows
                    int start = headerRowNum + 1;
                    if (focusedRow != null) {
                        // focusedRow is 1-based, because it is extracted from rowNum, whereas 'start' or 'i' is 0-based
                        // And we want the row before focusedRow
                        start = Math.max(focusedRow - 2, start);
                    }
                    int end = sheet.getLastRowNum();
                    if (maxRows != null) {
                        end = Math.min(start + maxRows - 1, end);
                    }
                    for (int i = start ; i <= end ; i++) {
                        int rowNum = i + 1;
                        boolean isFocused = focusedRow != null && focusedRow == rowNum;
                        rowConsumer.consumeRow(isFocused, rowNum, readRow(sheet.getRow(i), firstCellNum, lastCellNum, evaluator));
                    }
                    rowConsumer.endOfSheet(sheetName);
                    LOG.debug(clock.time("Done reading one sheet"));
                }
            }
            rowConsumer.endOfWorkbook();
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

    private List<String> readRow(Row row, short firstCellNum, short lastCellNum, FormulaEvaluator evaluator) {
        if (row != null) {
            List<String> values = Lists.newArrayList();
            for (int i = firstCellNum ; i < lastCellNum ; i++) {
                Cell cell = row.getCell(i);
                if (cell == null) {
                    values.add(null);
                } else {
                    values.add(computeCellValue(cell, evaluator));
                }
            }
            return values;
        } else {
            return null;
        }
    }

    private String computeCellValue(Cell cell, FormulaEvaluator evaluator){
        Object cellValue;
        try {
            CellValue computedFormulaValue = evaluator.evaluate(cell) ;
            if(computedFormulaValue != null){
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
        return (cellValue == null) ? null : Objects.toString(cellValue);
    }

    private static long readLong(Object value, long min, long max, long defaultValue) {
        if (value instanceof String) {
            try {
                long limit = Long.parseLong((String) value);
                if (limit < min) limit = min;
                if (limit > max) limit = max;
                return limit;
            } catch (NumberFormatException nfe) {
                return defaultValue;
            }
        } else {
            return defaultValue;
        }
    }

    public Long getRowLimit() {
        PluginSettings settings = pluginSettingsFactory.createGlobalSettings();
        Object value = settings.get(SETTINGS_ROW_LIMIT);
        return readLong(value, 1, MAX_ROWS_DEFAULT, MAX_ROWS_DEFAULT);
    }

    public void setRowLimit(Long limit) {
        if (accessModeService.isReadOnlyAccessModeEnabled()) {
            LOG.warn("PSEA settings were not saved because the instance is in read-only mode");
            return; // Don't save, silently.
        }
        PluginSettings settings = pluginSettingsFactory.createGlobalSettings();
        if (limit == null) {
            settings.remove(SETTINGS_ROW_LIMIT);
        } else {
            settings.put(SETTINGS_ROW_LIMIT, String.valueOf(limit));
        }
    }

    public Long getTimeLimit() {
        PluginSettings settings = pluginSettingsFactory.createGlobalSettings();
        Object value = settings.get(SETTINGS_TIME_LIMIT);
        return readLong(value, 1, TIME_LIMIT_MAX, TIME_LIMIT_DEFAULT);
    }

    public void setTimeLimit(Long limit) {
        if (accessModeService.isReadOnlyAccessModeEnabled()) {
            LOG.warn("PSEA settings were not saved because the instance is in read-only mode");
            return; // Don't save, silently.
        }
        PluginSettings settings = pluginSettingsFactory.createGlobalSettings();
        if (limit == null) {
            settings.remove(SETTINGS_TIME_LIMIT);
        } else {
            settings.put(SETTINGS_TIME_LIMIT, String.valueOf(limit));
        }
    }

    public long getDataLimit() {
        PluginSettings settings = pluginSettingsFactory.createGlobalSettings();
        Object value = settings.get(SETTINGS_DATA_LIMIT);
        return readLong(value, 1, DATA_LIMIT_MAX, DATA_LIMIT_DEFAULT);
    }

    public void setDataLimit(Long limit) {
        if (accessModeService.isReadOnlyAccessModeEnabled()) {
            LOG.warn("PSEA settings were not saved because the instance is in read-only mode");
            return; // Don't save, silently.
        }
        PluginSettings settings = pluginSettingsFactory.createGlobalSettings();
        if (limit == null) {
            settings.remove(SETTINGS_DATA_LIMIT);
        } else {
            settings.put(SETTINGS_DATA_LIMIT, String.valueOf(limit));
        }
    }
}
