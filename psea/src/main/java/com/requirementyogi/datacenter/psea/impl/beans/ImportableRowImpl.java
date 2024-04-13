package com.requirementyogi.datacenter.psea.impl.beans;

import com.requirementyogi.datacenter.psea.api.ImportableCell;
import com.requirementyogi.datacenter.psea.api.ImportableRow;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

public class ImportableRowImpl implements ImportableRow {

    private final int rowNum;
    private final boolean isFocused;
    private final List<ImportableCell> cells;

    public ImportableRowImpl(int rowNum, boolean isFocused, List<ImportableCell> cells) {
        this.rowNum = rowNum;
        this.isFocused = isFocused;
        this.cells = cells;
    }

    @Override
    public Integer getRowNum() {
        return rowNum;
    }

    @Override
    public boolean isFocused() {
        return false;
    }

    @Override
    public List<ImportableCell> getCells() {
        return cells;
    }

    @Override
    @Nullable
    public List<String> getCellsAsString(boolean merged) {
        if (cells == null) return null;
        return cells
                .stream()
                .map(ImportableCell::getValue)
                .collect(Collectors.toList());
    }
}
