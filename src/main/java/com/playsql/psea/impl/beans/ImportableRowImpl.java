package com.playsql.psea.impl.beans;

/*-
 * #%L
 * PSEA
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

import com.playsql.psea.api.ImportableCell;
import com.playsql.psea.api.ImportableRow;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

public class ImportableRowImpl implements ImportableRow {

    private final int rowNum;
    private final boolean isFocused;
    private final List<ImportableCell> cells;

    public ImportableRowImpl(int rowNum, boolean isFocused, List<ImportableCell> cells) {
        this.rowNum = rowNum;
        this.isFocused = isFocused;
        this.cells = cells;
    }

    @Override
    public Integer getRowNum() {
        return rowNum;
    }

    @Override
    public boolean isFocused() {
        return false;
    }

    @Override
    public List<ImportableCell> getCells() {
        return cells;
    }

    @Override
    @Nullable
    public List<String> getCellsAsString(boolean merged) {
        if (cells == null) return null;
        return cells
                .stream()
                .map(ImportableCell::getValue)
                .collect(Collectors.toList());
    }
}
