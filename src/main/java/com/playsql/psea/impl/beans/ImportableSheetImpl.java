package com.playsql.psea.impl.beans;

import com.playsql.psea.api.ImportableRow;
import com.playsql.psea.api.ImportableSheet;

public class ImportableSheetImpl implements ImportableSheet {

    private final String name;
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
