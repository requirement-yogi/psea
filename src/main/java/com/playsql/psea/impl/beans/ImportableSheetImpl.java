package com.playsql.psea.impl.beans;

/*-
 * #%L
 * PSEA
 * %%
 * Copyright (C) 2016 - 2024 Requirement Yogi S.A.S.U.
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

import com.playsql.psea.api.ImportableRow;
import com.playsql.psea.api.ImportableSheet;

import javax.annotation.Nullable;

public class ImportableSheetImpl implements ImportableSheet {

    private final String name;

    @Nullable
    private final ImportableRow headers;

    public ImportableSheetImpl(String sheetName, ImportableRow headers) {
        this.name = sheetName;
        this.headers = headers;
    }

    public String getName() {
        return name;
    }

    public ImportableRow getHeaderRow() {
        return headers;
    }
}
