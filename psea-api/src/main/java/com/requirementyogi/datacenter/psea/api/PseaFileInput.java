package com.requirementyogi.datacenter.psea.api;

import java.io.File;

public class PseaFileInput implements PseaInput {
    private final File file;

    /**
     * @deprecated since 1.9.2
     */
    public PseaFileInput(File file, String fileName) {
        this.file = file;
    }

    /**
     * @since PSEA 1.7
     */
    public PseaFileInput(File file) {
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    @Override
    public String getFileName() {
        return file.getName();
    }
}
