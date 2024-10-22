package com.requirementyogi.datacenter.psea.impl;

import com.requirementyogi.datacenter.psea.api.Row;
import com.requirementyogi.datacenter.psea.api.Value;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;

public class RowImpl implements Row {
    // Excel's maximum character limit per cell (32,767 characters)
    private static final int EXCEL_MAX_CELL_LENGTH = 32767;

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

            String contents = value.getValue();
            if (contents != null && contents.length() > EXCEL_MAX_CELL_LENGTH) {
                sheet.getWorkbook().addError(
                        "Sheet " + sheet.getName()
                        + ", cell " + sheet.getWorkbook().getCellReference(xlRow.getRowNum(), col)
                        + ": The value was abbreviated to " + EXCEL_MAX_CELL_LENGTH
                        + " bytes (original size: " + contents.length() + ")"
                );
                contents = StringUtils.abbreviate(contents, EXCEL_MAX_CELL_LENGTH);
            }
            xlCell.setCellValue(contents);
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
