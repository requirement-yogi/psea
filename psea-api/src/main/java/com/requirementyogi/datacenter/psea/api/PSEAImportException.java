package com.requirementyogi.datacenter.psea.api;

/**
 * A checked exception, for all situations where an import fails (most often when Apache POI fails)
 */
public class PSEAImportException extends Exception {
    public PSEAImportException() {
    }

    public PSEAImportException(String message) {
        super(message);
    }

    public PSEAImportException(String message, Throwable cause) {
        super(message, cause);
    }

    public PSEAImportException(Throwable cause) {
        super(cause);
    }

    public PSEAImportException(PseaInput workbookFile, Throwable e) {
        super("Exception while importing " + workbookFile.getFileName(), e);
    }
}
