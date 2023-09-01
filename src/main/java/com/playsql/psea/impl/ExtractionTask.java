package com.playsql.psea.impl;

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

import com.atlassian.activeobjects.external.ActiveObjects;
import com.playsql.psea.api.ExcelImportConsumer;
import com.playsql.psea.api.PSEAImportException;
import com.playsql.psea.api.PseaService;
import com.playsql.psea.utils.Utils;

import java.util.function.Consumer;

/**
 * Task that can extract a file and call a rowConsumer
 */
public abstract class ExtractionTask {

    protected final ExcelImportConsumer rowConsumer;
    protected final ActiveObjects ao;

    // 3 ways to skip parts of the spreadsheet
    protected final Integer maxRows;
    protected final String focusedSheet;
    protected final Integer focusedRow;
    protected final int maxRecordsPerTransaction;

    // Stateful fields
    protected Utils.Clock clock;

    protected ExtractionTask(ActiveObjects ao, ExcelImportConsumer rowConsumer) {
        this.ao = ao;
        this.rowConsumer = rowConsumer;
        this.maxRows = rowConsumer.getMaxRows();
        this.focusedSheet = rowConsumer.getFocusedSheet();
        this.focusedRow = rowConsumer.getFocusedRow();
        this.maxRecordsPerTransaction = rowConsumer.getMaxRecordsPerTransaction();
    }

    public abstract void extract(PseaService.PseaInput workbookFile) throws OutOfMemoryError, PSEAImportException;

}
