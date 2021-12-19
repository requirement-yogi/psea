package com.playsql.psea.impl;

/*
 * #%L
 * Play SQL Exports
 * %%
 * Copyright (C) 2016 Play SQL S.A.S.U.
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
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.*;

import java.util.Map;

public final class WorkbookAPIImpl implements WorkbookAPI {

    private final static org.apache.log4j.Logger LOG = Logger.getLogger(WorkbookAPIImpl.class);
    final XSSFWorkbook workbook;
    final Map<Style, CellStyle> styles = Maps.newHashMap();

    public WorkbookAPIImpl(XSSFWorkbook workbook) {
        this.workbook = workbook;

        // The colors
        IndexedColorMap colorMap = new DefaultIndexedColorMap();
        XSSFColor RED = new XSSFColor(new java.awt.Color(255,0, 0), colorMap);
        XSSFColor RED_CELL_COLOR = new XSSFColor(new java.awt.Color(172,80, 80), colorMap);

        // The fonts
        XSSFFont BOLD_FONT = workbook.createFont();
        BOLD_FONT.setBold(true);

        XSSFFont BOLD_RED_FONT = workbook.createFont();
        BOLD_RED_FONT.setBold(true);
        BOLD_RED_FONT.setColor(RED);

        XSSFFont BOLD_WHITE_FONT = workbook.createFont();
        BOLD_WHITE_FONT.setBold(true);
        BOLD_WHITE_FONT.setColor(IndexedColors.WHITE.getIndex());

        XSSFCellStyle STYLE_TH = workbook.createCellStyle();
        STYLE_TH.setFont(BOLD_FONT);
        styles.put(Style.TH, STYLE_TH);
        styles.put(Style.ID_COLUMN, STYLE_TH);

        XSSFFont WORKBOOK_TITLE_FONT = workbook.createFont();
        WORKBOOK_TITLE_FONT.setBold(true);
        WORKBOOK_TITLE_FONT.setFontHeightInPoints((short) 16);

        XSSFCellStyle STYLE_WORKBOOK_TITLE = workbook.createCellStyle();
        STYLE_WORKBOOK_TITLE.setFont(WORKBOOK_TITLE_FONT);
        //STYLE_WORKBOOK_TITLE.setAlignment(CellStyle.ALIGN_LEFT);
        STYLE_WORKBOOK_TITLE.setVerticalAlignment(VerticalAlignment.CENTER);
        styles.put(Style.WORKBOOK_TITLE, STYLE_WORKBOOK_TITLE);

        XSSFCellStyle STYLE_RED_CELL = workbook.createCellStyle();
        STYLE_RED_CELL.setFont(BOLD_WHITE_FONT);
        STYLE_RED_CELL.setFillForegroundColor(RED_CELL_COLOR);
        STYLE_RED_CELL.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styles.put(Style.RED_CELL, STYLE_RED_CELL);

        XSSFCellStyle STYLE_MIRROR_CELL = workbook.createCellStyle();
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
}
