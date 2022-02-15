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

import com.google.common.collect.Maps;
import com.playsql.psea.api.Sheet;
import com.playsql.psea.api.WorkbookAPI;
import com.playsql.psea.utils.Utils;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.IndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFColor;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class WorkbookAPIImpl implements WorkbookAPI {

    private final static org.apache.log4j.Logger LOG = Logger.getLogger(WorkbookAPIImpl.class);

    /**
     * Max allowed time for generating an excel file
     */
    private static final Long MAX_DURATION = TimeUnit.MINUTES.toMillis(2);

    private final SXSSFWorkbook workbook;

    /**
     * max authorized rows
     */
    private final Integer rowLimit;
    /**
     * max time to achieve export, in milliseconds
     */
    private final Integer timeLimit;
    private final Utils.Clock timer;
    private final Map<Style, CellStyle> styles = Maps.newHashMap();

    public WorkbookAPIImpl(SXSSFWorkbook workbook, @Nullable Integer rowLimit, @Nullable Integer timeLimit) {
        this.workbook = workbook;
        this.rowLimit = rowLimit;
        this.timeLimit = timeLimit != null ? timeLimit : MAX_DURATION.intValue();
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

        CellStyle STYLE_ERROR_CELL = workbook.createCellStyle();
        STYLE_ERROR_CELL.setFont(BOLD_RED_FONT);
        styles.put(Style.ERROR_CELL, STYLE_ERROR_CELL);
    }

    @Override
    public Sheet newSheet(String name) {
        return new SheetImpl(this, workbook.createSheet(name));
    }

    @Override
    public Sheet getSheet(String title) {
        SXSSFSheet sheet = workbook.getSheet(title);
        if (sheet == null) return null;
        return new SheetImpl(this, sheet);
    }

    public SXSSFWorkbook getWorkbook() {
        return workbook;
    }

    public Map<Style, CellStyle> getStyles() {
        return styles;
    }

    public Integer getRowLimit() {
        return rowLimit;
    }

    public void checkTimer() {
        long elapsedTime = timer.timeMillis();
        if (elapsedTime > timeLimit) {
            throw new IllegalArgumentException(
                    "Export time (" + elapsedTime + " ms) exceeded max configured time (" + timeLimit + " ms)");
        }
    }
}
