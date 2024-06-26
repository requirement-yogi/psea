package com.requirementyogi.datacenter.psea.api;

import javax.annotation.Nullable;
import java.util.List;

public interface ImportableRow {

    /**
     * Whether the row matches `focusedElements`
     */
    boolean isFocused();

    /**
     * The row number, counted as in the original file, 1-based. It means rowNum is never 1, because
     * even if title are stacked at the top of the sheet, the first row is still the titles
     */
    Integer getRowNum();

    /**
     * The list of cell values, or null if the row is empty
     */
    @Nullable
    List<ImportableCell> getCells();

    /**
     * Returns cells as string, like in the olden times
     *
     * @param merged if true, then merged cells will have the value of the merged cell.
     *               Otherwise the value stays null.
     *
     * @since 1.9
     */
    @Nullable
    List<String> getCellsAsString(boolean merged);
}
