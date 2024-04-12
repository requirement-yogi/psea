package com.playsql.psea.impl;

import com.playsql.psea.api.Row;
import com.playsql.psea.api.Value;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;

public class RowImpl implements Row {
    private final SheetImpl sheet;
    private final XSSFRow xlRow;
    public RowImpl(SheetImpl sheet, XSSFRow xlRow) {
        this.sheet = sheet;
        this.xlRow = xlRow;
    }

    @Override
    public void setCell(int col, Value value) {
        if (value != null) {
            sheet.addSize(value);
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
