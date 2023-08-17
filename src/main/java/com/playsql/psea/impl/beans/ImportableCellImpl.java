package com.playsql.psea.impl.beans;

import com.playsql.psea.api.ImportableCell;

public class ImportableCellImpl implements ImportableCell {
    private final int index;
    private final String value;

    public ImportableCellImpl(int index, String value) {
        this.index = index;
        this.value = value;
    }

    @Override
    public Integer getIndex() {
        return index;
    }

    @Override
    public String getValue() {
        return value;
    }
}
