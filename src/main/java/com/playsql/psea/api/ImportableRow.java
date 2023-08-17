package com.playsql.psea.api;

/*
 * #%L
 * Play SQL Exports
 * %%
 * Copyright (C) 2016 - 2023 Requirement Yogi S.A.S.U.
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
