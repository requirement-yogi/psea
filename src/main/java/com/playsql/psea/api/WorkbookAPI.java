package com.playsql.psea.api;

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
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.xssf.usermodel.*;

import java.util.Map;

public final class WorkbookAPI {
    final XSSFWorkbook workbook;
    final Map<Style, CellStyle> styles = Maps.newHashMap();

    public WorkbookAPI(XSSFWorkbook workbook) {
        this.workbook = workbook;
        XSSFFont BOLD_FONT = workbook.createFont();
        BOLD_FONT.setBold(true);

        XSSFCellStyle STYLE_TH = workbook.createCellStyle();
        STYLE_TH.setFont(BOLD_FONT);
        styles.put(Style.TH, STYLE_TH);

        XSSFFont WORKBOOK_TITLE_FONT = workbook.createFont();
        WORKBOOK_TITLE_FONT.setBold(true);
        WORKBOOK_TITLE_FONT.setFontHeightInPoints((short) 16);

        XSSFCellStyle STYLE_WORKBOOK_TITLE = workbook.createCellStyle();
        STYLE_WORKBOOK_TITLE.setFont(WORKBOOK_TITLE_FONT);
        STYLE_WORKBOOK_TITLE.setAlignment(CellStyle.ALIGN_CENTER);
        STYLE_WORKBOOK_TITLE.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
        styles.put(Style.WORKBOOK_TITLE, STYLE_WORKBOOK_TITLE);

        XSSFCellStyle STYLE_RED_CELL = workbook.createCellStyle();
        STYLE_RED_CELL.setFont(BOLD_FONT);
        styles.put(Style.RED_CELL, STYLE_RED_CELL);
    }

    public Sheet newSheet(String name) {
        return new Sheet(this, workbook.createSheet(name));
    }

    public enum Style {
        TH, WORKBOOK_TITLE, RED_CELL;
    }

    public abstract static class Value {
        protected abstract void setCell(XSSFCell xlCell, Map<Style, CellStyle> styles);
    }
    public static class StringValue extends Value {
        private final Style format;
        private final String value;

        public StringValue(String value) {
            this.value = value;
            this.format = null;
        }

        public StringValue(Style style, String value) {
            this.format = style;
            this.value = value;
        }

        @Override
        protected void setCell(XSSFCell xlCell, Map<Style, CellStyle> styles) {
            xlCell.setCellValue(value);
                    CellStyle style = styles.get(format);
                    if (style != null)
                        xlCell.setCellStyle(style);
        }
    }

    public static class Sheet {
        private final WorkbookAPI workbook;
        private final XSSFSheet sheet;
        private int rowNum = 0;

        private Sheet(WorkbookAPI workbook, XSSFSheet sheet) {
            this.workbook = workbook;
            this.sheet = sheet;
        }

        public void setData(Value[][] values, String[][] formats) {
            if (values != null) {
                for (int row = 0 ; row < values.length ; row++) {
                    throw new RuntimeException("Not implemented");
                }
            }
        }

        public void addRow(Value[] values, Style[] formats) {
            XSSFRow xlRow = sheet.createRow(rowNum++);
            for (int col = 0 ; col < values.length ; col++) {
                XSSFCell xlCell = xlRow.createCell(col);
                values[col].setCell(xlCell, workbook.styles);
                /*if (formats.length > col) {
                    CellStyle style = workbook.styles.get(formats[col]);
                    if (style != null)
                        xlCell.setCellStyle(style);
                }*/
            }
        }
    }
}
