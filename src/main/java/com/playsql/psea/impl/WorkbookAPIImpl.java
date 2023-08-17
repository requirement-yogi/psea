package com.playsql.psea.impl;

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

import com.google.common.collect.Maps;
import com.playsql.psea.api.Sheet;
import com.playsql.psea.api.WorkbookAPI;
import com.playsql.psea.dto.PseaLimitException;
import com.playsql.psea.utils.Utils;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.function.Consumer;

public final class WorkbookAPIImpl implements WorkbookAPI {

    private final static org.apache.log4j.Logger LOG = Logger.getLogger(WorkbookAPIImpl.class);

    private final XSSFWorkbook workbook;

    /**
     * max authorized rows
     */
    private final long rowLimit;

    /**
     * max time to achieve export, in milliseconds
     */
    private final long timeLimit;

    /**
     * the current size of the export. The unit is kinda unknown, basically characters.
     */
    private long currentSize;

    /**
     * A function to save the current size of the export.
     *
     * It throws a RuntimeException if the size goes overboard,
     * It will allow some post-mortem by the admins in case of memory error.
     * */
    @Nonnull
    private final Consumer<Long> saveSize;

    private final Utils.Clock timer;
    private final Map<Style, CellStyle> styles = Maps.newHashMap();

    public WorkbookAPIImpl(XSSFWorkbook workbook,
                           long rowLimit,
                           long timeLimit,
                           @Nonnull Consumer<Long> saveSize
    ) {
        this.workbook = workbook;
        this.rowLimit = rowLimit;
        this.timeLimit = timeLimit;
        this.saveSize = saveSize;
        this.timer = Utils.Clock.start();

        // The colors
        IndexedColorMap colorMap = new DefaultIndexedColorMap();
        XSSFColor RED = new XSSFColor(new java.awt.Color(255, 0, 0), colorMap);
        XSSFColor RED_CELL_COLOR = new XSSFColor(new java.awt.Color(172, 80, 80), colorMap);

        // The fonts
        Font BOLD_FONT = workbook.createFont();
        BOLD_FONT.setBold(true);

        Font BOLD_RED_FONT = workbook.createFont();
        BOLD_RED_FONT.setBold(true);
        BOLD_RED_FONT.setColor(RED.getIndex());

        Font BOLD_WHITE_FONT = workbook.createFont();
        BOLD_WHITE_FONT.setBold(true);
        BOLD_WHITE_FONT.setColor(IndexedColors.WHITE.getIndex());

        CellStyle STYLE_TH = workbook.createCellStyle();
        STYLE_TH.setFont(BOLD_FONT);
        styles.put(Style.TH, STYLE_TH);
        styles.put(Style.ID_COLUMN, STYLE_TH);

        Font WORKBOOK_TITLE_FONT = workbook.createFont();
        WORKBOOK_TITLE_FONT.setBold(true);
        WORKBOOK_TITLE_FONT.setFontHeightInPoints((short) 16);

        CellStyle STYLE_WORKBOOK_TITLE = workbook.createCellStyle();
        STYLE_WORKBOOK_TITLE.setFont(WORKBOOK_TITLE_FONT);
        //STYLE_WORKBOOK_TITLE.setAlignment(CellStyle.ALIGN_LEFT);
        STYLE_WORKBOOK_TITLE.setVerticalAlignment(VerticalAlignment.CENTER);
        styles.put(Style.WORKBOOK_TITLE, STYLE_WORKBOOK_TITLE);

        CellStyle STYLE_RED_CELL = workbook.createCellStyle();
        STYLE_RED_CELL.setFont(BOLD_WHITE_FONT);
        STYLE_RED_CELL.setFillForegroundColor(RED_CELL_COLOR.getIndex());
        STYLE_RED_CELL.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styles.put(Style.RED_CELL, STYLE_RED_CELL);

        CellStyle STYLE_MIRROR_CELL = workbook.createCellStyle();
        STYLE_MIRROR_CELL.setFillForegroundColor(IndexedColors.BLUE_GREY.getIndex());
        STYLE_MIRROR_CELL.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styles.put(Style.MIRROR_CELL, STYLE_MIRROR_CELL);

        XSSFCellStyle STYLE_ERROR_CELL = workbook.createCellStyle();
        STYLE_ERROR_CELL.setFont(BOLD_RED_FONT);
        styles.put(Style.ERROR_CELL, STYLE_ERROR_CELL);
    }

    @Override
    public Sheet newSheet(String name) {
        return new SheetImpl(this, workbook.createSheet(name));
    }

    @Override
    public Sheet getSheet(String title) {
        XSSFSheet sheet = workbook.getSheet(title);
        if (sheet == null) return null;
        return new SheetImpl(this, sheet);
    }

    public XSSFWorkbook getWorkbook() {
        return workbook;
    }

    public Map<Style, CellStyle> getStyles() {
        return styles;
    }

    public long getRowLimit() {
        return rowLimit;
    }

    public void checkTimer() {
        long elapsedTime = timer.timeMillis();
        if (elapsedTime > timeLimit) {
            throw new PseaLimitException(elapsedTime, timeLimit, "time", "ms");
        }
    }

    /** Adds the size of this cell to the total size of the file, and throw an exception if the Excel file is too big. */
    public void addSize(long size) {
        this.currentSize += size;
        this.saveSize.accept(this.currentSize);
    }

    public long getDataSize() {
        return this.currentSize;
    }

    /**
     * Write an error message on the first sheet, first cell.
     * This method bypasses all the limits, because error messages are necessary.
     */
    public void writeErrorMessageOnFirstSheet(String message) {
        XSSFSheet firstSheet = workbook.getSheetAt(0);
        XSSFRow xlRow = firstSheet.createRow(0);
        xlRow.setHeight((short) Math.max(xlRow.getHeight() * 4, 30));
        XSSFCell cell = xlRow.createCell(0);
        cell.setCellValue("ERROR, the export was interrupted and aborted: " + message);
        cell.setCellStyle(styles.get(Style.ERROR_CELL));
    }
}
