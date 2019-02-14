package com.playsql.psea.api;

/*
 * #%L
 * Play SQL Exports
 * %%
 * Copyright (C) 2016 - 2018 Play SQL S.A.S.U.
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

// TODO OSGI can’t load from RY

public abstract class ExcelImportConsumer {

    private Integer maxRows;
    private Object[] focusedElements;
    private String[] inactiveSheets;

    public ExcelImportConsumer() {
    }

    public ExcelImportConsumer(Integer maxRows, Object[] focusedElements, String[] inactiveSheets) {
        this.maxRows = maxRows;
        this.focusedElements = focusedElements;
        this.inactiveSheets = inactiveSheets;
    }

    public abstract void consumeNewSheet(ImportableSheet sheet);
    public abstract void consumeRow(ImportableRow row);

    public Integer getMaxRows() {
        return maxRows;
    }

    public Object[] getFocusedElements() {
        return focusedElements;
    }

    public String[] getInactiveSheets() {
        return inactiveSheets;
    }
}
