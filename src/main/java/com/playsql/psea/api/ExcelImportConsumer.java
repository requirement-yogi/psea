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

import java.util.Map;
import java.util.function.Consumer;

public class ExcelImportConsumer {

    protected Workbook optionalOutput;
    protected Map<String, Object> rowConsumptionInOut;
    protected Consumer<Workbook.Row> rowConsumer;

    public ExcelImportConsumer() {
    }

    public ExcelImportConsumer(Consumer<Workbook.Row> rowConsumer) {
        this.rowConsumer = rowConsumer;
    }

    public Workbook getOptionalOutput() {
        return optionalOutput;
    }
    public void setOptionalOutput(Workbook optionalOutput) {
        this.optionalOutput = optionalOutput;
    }

    public Map<String, Object> getRowConsumptionInOut() {
        return rowConsumptionInOut;
    }

    public void setRowConsumptionInOut(Map<String, Object> rowConsumptionInOut) {
        this.rowConsumptionInOut = rowConsumptionInOut;
    }

    public void consumeRow(Workbook.Row row) {
        if (rowConsumer != null)
            rowConsumer.accept(row);
    }

    public Consumer<Workbook.Row> getRowConsumer() {
        return rowConsumer;
    }

    public void setRowConsumer(Consumer<Workbook.Row> rowConsumer) {
        this.rowConsumer = rowConsumer;
    }
}
