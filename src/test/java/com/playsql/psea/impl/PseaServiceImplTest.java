package com.playsql.psea.impl;

import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static com.playsql.psea.impl.PseaTestUtils.ACCESS_MODE_SERVICE;
import static org.junit.Assert.*;

public class PseaServiceImplTest {

    private PseaServiceImpl service = new PseaServiceImpl(PseaTestUtils.PLUGIN_SETTINGS, ACCESS_MODE_SERVICE);

    @Test
    public void testDeleteFile() throws IOException {
        File fileWrongPrefixWrongSuffix = File.createTempFile("whatever", ".txt");
        fileWrongPrefixWrongSuffix.deleteOnExit();
        assertFalse(service.deleteFile(fileWrongPrefixWrongSuffix));

        File fileGoodPrefixWrongSuffix = File.createTempFile("excel-export-", ".txt");
        fileGoodPrefixWrongSuffix.deleteOnExit();
        assertFalse(service.deleteFile(fileGoodPrefixWrongSuffix));

        File fileGoodPrefixGoodSuffix = File.createTempFile("excel-export-", ".xlsx");
        fileGoodPrefixGoodSuffix.deleteOnExit();
        assertTrue(service.deleteFile(fileGoodPrefixGoodSuffix));
    }
}