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

public class ImportableCellImpl implements ImportableCell {
    private final int index;
    private final String value;
    private boolean isMerged;
    private final String mergedValue;

    public ImportableCellImpl(int index, boolean isMerged, String value, String mergedValue) {
        this.index = index;
        this.value = value;
        this.isMerged = isMerged;
        this.mergedValue = mergedValue;
    }

    @Override
    public Integer getIndex() {
        return index;
    }

    @Override
    public String getValue() {
        return value;
    }

    public boolean isMerged() {
        return isMerged;
    }

    public String getMergedValue() {
        return mergedValue;
    }
}
