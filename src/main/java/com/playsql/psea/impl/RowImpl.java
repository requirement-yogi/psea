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

import com.playsql.psea.api.Row;
import com.playsql.psea.api.Value;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;

public class RowImpl implements Row {
    private SheetImpl sheet;
    private final XSSFRow xlRow;

    public RowImpl(SheetImpl sheet, XSSFRow xlRow) {
        this.sheet = sheet;
        this.xlRow = xlRow;
    }

    @Override
    public void setCell(int col, Value value) {
        if (value != null) {
            XSSFCell xlCell = xlRow.createCell(col);

            xlCell.setCellValue(value.getValue());
            CellStyle style = sheet.getWorkbook().getStyles().get(value.getFormat());
            if (style != null) {
                xlCell.setCellStyle(style);
            }
            if (value.getHref() != null) {
                Hyperlink link = sheet.getHelper().createHyperlink(HyperlinkType.URL);
                link.setAddress(value.getHref());
                xlCell.setHyperlink(link);
            }
        }
    }
}
