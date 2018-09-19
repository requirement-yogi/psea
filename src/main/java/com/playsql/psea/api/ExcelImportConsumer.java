package com.playsql.psea.api;

// TODO OSGI can’t load from RY

import java.util.Map;
import java.util.function.Consumer;

public abstract class ExcelImportConsumer implements Consumer<Workbook.Row> {
    protected Workbook output;
    protected Map<String, Object> rowConsumptionInOut;

//
//    public ExcelImportConsumer(Workbook output, Map<String, Object> rowConsumptionInOut) {
//        this.output = output;
//        this.rowConsumptionInOut = rowConsumptionInOut;
//    }
//
//    public ExcelImportConsumer(Map<String, Object> rowConsumptionInOut) {
//        this.rowConsumptionInOut = rowConsumptionInOut;
//    }
//
    public ExcelImportConsumer() {
    }

    public Workbook getOutput() {
        return output;
    }

    public void setOutput(Workbook output) {
        this.output = output;
    }

    public Map<String, Object> getRowConsumptionInOut() {
        return rowConsumptionInOut;
    }

    public void setRowConsumptionInOut(Map<String, Object> rowConsumptionInOut) {
        this.rowConsumptionInOut = rowConsumptionInOut;
    }
}
