package com.requirementyogi.datacenter.psea.api;

import java.util.List;

public interface WorkbookAPI {
    Sheet newSheet(String name);

    Sheet getSheet(String title);

    /**
     * @return the list of errors met while building the workbook.
     * At the time of writing this bean, there is only the 32KB limit of content per cell which may create errors.
     *
     * @since 2.0.2
     */
    List<String> getErrors();

    /**
     * @return the list of warnings met while building the workbook.
     *
     * @since 2.0.2
     */
    List<String> getWarnings();

    /**
     * Returns a reference in the form "A1"
     *
     * @param column the 0-based column number
     * @param row the 0-based row number
     * @return a string such as "A1"
     */
    String getCellReference(int row, int column);

    enum Style {
        TH,

        ID_COLUMN,

        WORKBOOK_TITLE,

        /**
         * @deprecated since 2.0.2
         * @see #ERROR_CELL
         */
        @Deprecated
        RED_CELL,

        /** The cell in a dependency matrix which is in the diagonal */
        MIRROR_CELL,

        /**
         *
         * A warning cell with yellow background
         * @since 2.0.1
         */
        WARNING_CELL,

        /**
         *
         * A cell with red text
         * @since 1.5
         */
        ERROR_CELL
    }
}
