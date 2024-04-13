package com.playsql.psea.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.playsql.psea.api.ExcelImportConsumer;
import com.playsql.psea.api.PSEAImportException;
import com.playsql.psea.api.PseaInput;
import com.playsql.psea.utils.Utils;

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

    public abstract void extract(PseaInput workbookFile) throws OutOfMemoryError, PSEAImportException;

}
