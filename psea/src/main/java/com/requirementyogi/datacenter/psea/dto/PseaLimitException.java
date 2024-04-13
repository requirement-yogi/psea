package com.requirementyogi.datacenter.psea.dto;

public class PseaLimitException extends RuntimeException {

    /** The limit not to cross */
    private final long limit;

    /** The cross */
    private final long currentSize;

    private final String limitName;
    private final String unit;

    public PseaLimitException(long currentSize, long limit, String limitName, String unit) {
        super(
                "The Excel export reached a hard limit for '" + limitName + "': "
                        + currentSize + " " + unit + " instead of " + limit + " " + unit
        );
        this.limit = limit;
        this.currentSize = currentSize;
        this.limitName = limitName;
        this.unit = unit;
    }

}
