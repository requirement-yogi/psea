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
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

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
        styles.put(Style.ID_COLUMN, STYLE_TH);

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

        XSSFCellStyle STYLE_MIRROR_CELL = workbook.createCellStyle();
        STYLE_MIRROR_CELL.setFillBackgroundColor(IndexedColors.BLUE_GREY.getIndex());
        STYLE_MIRROR_CELL.setFillPattern(CellStyle.NO_FILL); // or SOLID_FOREGROUND?
        styles.put(Style.MIRROR_CELL, STYLE_MIRROR_CELL);
    }

    public Sheet newSheet(String name) {
        return new Sheet(this, workbook.createSheet(name));
    }

    public Sheet getSheet(String title) {
        XSSFSheet sheet = workbook.getSheet(title);
        if (sheet == null) return null;
        return new Sheet(this, sheet);
    }

    public enum Style {
        TH, ID_COLUMN,
        WORKBOOK_TITLE,
        RED_CELL,
        /** The cell in a dependency matrix which is in the diagonal */
        MIRROR_CELL
    }

    public static class Value {
        private final Style format;
        private final String value;
        private final String href;

        public Value(Object value) {
            this.value = Objects.toString(value);
            this.format = null;
            href = null;
        }

        public Value(Style format, Object value) {
            this.format = format;
            this.value = Objects.toString(value);
            href = null;
        }

        public Value(Style format, Object value, String href) {
            this.format = format;
            this.value = Objects.toString(value);
            this.href = href;
        }

        public Style getFormat() {
            return format;
        }

        public String getValue() {
            return value;
        }

        public String getHref() {
            return href;
        }

        @Override
        public String toString() {
            return String.format("%s (%s, %s)",
                value,
                Objects.toString(format),
                href);
        }
    }

    public static class Sheet {
        private final WorkbookAPI workbook;
        private final XSSFCreationHelper helper;
        private final XSSFSheet sheet;
        private int rowNum = 1;

        private Sheet(WorkbookAPI workbook, XSSFSheet sheet) {
            this.workbook = workbook;
            this.helper = workbook.workbook.getCreationHelper();
            this.sheet = sheet;
        }

        public Row addRow(List<? extends Value> values) {
            return addRow(rowNum++, values);
        }
        public Row addRow(int position, List<? extends Value> values) {
            XSSFRow xlRow = sheet.createRow(position);
            Row row = new Row(xlRow);
            for (int col = 0 ; col < values.size() ; col++) {
                row.setCell(col, values.get(col));
            }
            return row;
        }

        public void autoSizeHeaders() {
            XSSFRow row0 = sheet.getRow(0);
            if (row0 != null) {
                int col = 0;

                XSSFCell cell;
                while (null != (cell = row0.getCell(col))) {
                    String rawValue = cell.getRawValue();
                    if (StringUtils.isNotBlank(rawValue)) {
                        sheet.autoSizeColumn(col);
                        /*final int CHARACTER_WIDTH = 256;
                        final int MAX_COLUMN_WIDTH = 255 * CHARACTER_WIDTH;
                        sheet.setColumnWidth(col, Math.min(sheet.getColumnWidth(col) + CHARACTER_WIDTH * 3, MAX_COLUMN_WIDTH));*/
                    }
                }
            }
        }

        public void setHeightInPoints(int rowNumber, float height) {
            XSSFRow row = sheet.getRow(rowNumber);
            if (row != null) {
                row.setHeightInPoints(height);
            }
        }

        public class Row {
            private final XSSFRow xlRow;

            public Row(XSSFRow xlRow) {
                this.xlRow = xlRow;
            }

            public void setCell(int col, Value value) {
                XSSFCell xlCell = xlRow.createCell(col);

                xlCell.setCellValue(value.getValue());
                CellStyle style = workbook.styles.get(value.getFormat());
                if (style != null) {
                    xlCell.setCellStyle(style);
                }
                if (value.getHref() != null) {
                    Hyperlink link = helper.createHyperlink(Hyperlink.LINK_URL);
                    link.setAddress(value.getHref());
                    xlCell.setHyperlink(link);
                }
            }
        }

        public void freezePanes(int colSplit, int rowSplit, int leftmostColumn, int topRow) {
            sheet.createFreezePane(colSplit, rowSplit, leftmostColumn, topRow);
        }
    }
}
