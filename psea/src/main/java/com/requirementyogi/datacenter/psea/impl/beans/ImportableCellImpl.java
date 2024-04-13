package com.requirementyogi.datacenter.psea.impl.beans;

import com.requirementyogi.datacenter.psea.api.ImportableCell;

public class ImportableCellImpl implements ImportableCell {
    private final int index;
    private final String value;
    private boolean isMerged;

    /**
     * The value if this cell belongs to a merged area, otherwise null
     */
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
