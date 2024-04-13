package com.requirementyogi.datacenter.psea.api;

public interface WorkbookAPI {
    Sheet newSheet(String name);

    Sheet getSheet(String title);

    enum Style {
        TH, ID_COLUMN,
        WORKBOOK_TITLE,
        RED_CELL,
        /** The cell in a dependency matrix which is in the diagonal */
        MIRROR_CELL,
        /** A cell with red text - Since 1.5 */
        ERROR_CELL
    }
}
