package com.requirementyogi.datacenter.psea.api;

import java.io.InputStream;

public class PseaInputStream implements PseaInput {
    private final InputStream inputStream;
    private final String fileName;

    public PseaInputStream(InputStream inputStream, String fileName) {
        this.inputStream = inputStream;
        this.fileName = fileName;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    @Override
    public String getFileName() {
        return fileName;
    }
}
