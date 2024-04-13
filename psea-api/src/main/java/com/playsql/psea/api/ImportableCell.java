package com.playsql.psea.api;

public interface ImportableCell {
    Integer getIndex();
    String getValue();

    /**
     * True if the cell is part of a merged area
     */
    boolean isMerged();

    /**
     * If isMerged(), then this is the value of the merged area
     * @return
     */
    String getMergedValue();
}
