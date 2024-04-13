package com.requirementyogi.datacenter.psea.api;

import java.util.Objects;

public class Value {
    private final WorkbookAPI.Style format;
    private final String value;
    private final String href;

    public Value(Object value) {
        this.value = Objects.toString(value);
        this.format = null;
        this.href = null;
    }

    public Value(WorkbookAPI.Style format, Object value) {
        this.format = format;
        this.value = Objects.toString(value);
        href = null;
    }

    public Value(WorkbookAPI.Style format, Object value, String href) {
        this.format = format;
        this.value = Objects.toString(value);
        this.href = href;
    }

    public WorkbookAPI.Style getFormat() {
        return format;
    }

    public String getValue() {
        return value;
    }

    public String getHref() {
        return href;
    }

    public String toString() {
        return String.format("%s (%s, %s)",
            value,
            Objects.toString(format),
            href);
    }
}
