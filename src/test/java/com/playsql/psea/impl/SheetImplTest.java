package com.playsql.psea.impl;

import com.google.common.collect.Lists;
import com.playsql.psea.api.Value;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class SheetImplTest {

    private SXSSFWorkbook workbook = new SXSSFWorkbook();

    @Before
    public void setUp() {
        workbook = new SXSSFWorkbook();
    }

    @After
    public void tearDown() throws Exception {
        workbook.dispose();
        workbook.close();
    }

    @Test
    public void testRowLimit() {
        WorkbookAPIImpl workbookAPI = new WorkbookAPIImpl(workbook, 1, 1000);
        SheetImpl sheet = new SheetImpl(workbookAPI, workbook.createSheet("foo"));

        sheet.addRow(Lists.newArrayList(new Value("v1")));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> sheet.addRow(Lists.newArrayList(new Value("v2"))));
        assertEquals("Row number (2) is outside the configured range (0..1)", e.getMessage());
    }

    @Test
    public void testTimeLimit() throws InterruptedException {
        WorkbookAPIImpl workbookAPI = new WorkbookAPIImpl(workbook, 10, 1000); // takes 300ms
        SheetImpl sheet = new SheetImpl(workbookAPI, workbook.createSheet("foo")); // takes 400ms
        sheet.addRow(Lists.newArrayList(new Value("v1")));
        TimeUnit.MILLISECONDS.sleep(1000);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> sheet.addRow(Lists.newArrayList(new Value("v2"))));
        assertThat(e.getMessage(), CoreMatchers.containsString("exceeded max configured time (1000 ms)"));
    }

}