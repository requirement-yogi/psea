package com.playsql.psea.utils;

/*-
 * #%L
 * PSEA
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

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Formatter, to convert XSSF-formatted cells into HTML
 * <p>
 * - It adds {@literal <b> <s> <i> <u> <span style="color: rgb"> ...}
 * - It escapes the characters of the string
 */
public class XSSFFormatter {

    public static String formatToHtml(XSSFCell cell) {
        XSSFRichTextString value = cell.getRichStringCellValue();
        int numFormattingRuns = value.numFormattingRuns();
        String rawString = value.getString();

        FormattingWriter writer = new FormattingWriter(rawString);
        for (int i = 0 ; i < numFormattingRuns ; i++) {
            int nextIndex = value.getIndexOfFormattingRun(i);
            writer.flushUntil(nextIndex)
                  .startStyles(value.getFontOfFormattingRun(i));
        }
        writer.flushUntil(null);
        return writer.getResultHtml();
    }


    /**
     * Writer for HTML, which can remember opened tags and close them.
     */
    public static class FormattingWriter {

        final String initialString;
        /**
         * Result, in HTML
         */
        final StringBuilder result = new StringBuilder();
        int start = 0;
        List<FormattingElement> styles = Lists.newArrayList();

        public FormattingWriter(String initialString) {
            this.initialString = initialString;
        }

        public void startStyles(XSSFFont font) {
            if (font.getBold()) styles.add(new FormattingElement("b", false));
            if (font.getItalic()) styles.add(new FormattingElement("i", false));
            if (font.getUnderline() > 0) styles.add(new FormattingElement("u", false));
            if (font.getStrikeout()) styles.add(new FormattingElement("s", false));
            List<String> cssStyles = Lists.newArrayList();
            if (font.getXSSFColor() != null) {
                XSSFColor color = font.getXSSFColor();
                byte[] colors = color.getARGB();
                if (colors.length == 4) {
                    byte alpha = colors[0];
                    byte red = colors[1];
                    byte green = colors[2];
                    byte blue = colors[3];
                    if (alpha == 127) {
                        cssStyles.add("color:rgb(" + red + "," + green + "," + blue + ")");
                    } else {
                        cssStyles.add("color:rgba(" + red + "," + green + "," + blue + "," + alpha + ")");
                    }

                }
            }
            if (!cssStyles.isEmpty()) {
                styles.add(new FormattingElement(StringUtils.join(cssStyles, ";"), true));
            }
            for (FormattingElement element : styles) {
                element.write(result);
            }
        }

        public FormattingWriter flushUntil(Integer nextIndex) {
            String textToWrite = nextIndex != null
                    ? initialString.substring(start, nextIndex)
                    : initialString.substring(start);
            result.append(StringEscapeUtils.escapeHtml4(textToWrite));

            // Then we close tags in reverse orders
            Collections.reverse(styles);
            for (FormattingElement element : styles) {
                element.writeClosingTag(result);
            }
            return this;
        }

        public String getResultHtml() {
            return result.toString();
        }
    }

    public static class FormattingElement {
        final String tag;
        /** True if it's a css-style, applied though a {@literal <span>}; false if it's an HTML tag */
        final boolean css;

        public FormattingElement(String html, boolean css) {
            this.tag = html;
            this.css = css;
        }

        public void write(StringBuilder sb) {
            if (css) {
                sb.append("<span style=\"").append(StringEscapeUtils.escapeHtml4(this.tag)).append("\">");
            } else {
                sb.append("<").append(this.tag).append(">");
            }
        }

        public void writeClosingTag(StringBuilder sb) {
            if (css) {
                sb.append("<span>");
            } else {
                sb.append("</").append(this.tag).append(">");
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof FormattingElement)) return false;
            FormattingElement that = (FormattingElement) o;
            return css == that.css && Objects.equals(tag, that.tag);
        }

        @Override
        public int hashCode() {
            return Objects.hash(tag, css);
        }
    }
}
