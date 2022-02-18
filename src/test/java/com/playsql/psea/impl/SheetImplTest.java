package com.playsql.psea.impl;

import com.google.common.collect.Lists;
import com.playsql.psea.api.Value;
import com.playsql.psea.dto.PseaLimitException;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class SheetImplTest {

    private XSSFWorkbook workbook;

    @Before
    public void setUp() {
        workbook = new XSSFWorkbook();
    }

    @After
    public void tearDown() throws Exception {
        workbook.close();
    }

    @Test
    public void testRowLimit() {
        WorkbookAPIImpl workbookAPI = new WorkbookAPIImpl(workbook, 1, 1000, saveSize -> {});
        SheetImpl sheet = new SheetImpl(workbookAPI, workbook.createSheet("foo"));

        sheet.addRow(Lists.newArrayList(new Value("v1")));
        PseaLimitException e = assertThrows(PseaLimitException.class, () -> sheet.addRow(Lists.newArrayList(new Value("v2"))));
        assertEquals("The Excel export reached a hard limit for 'rows': 2 rows instead of 1 rows", e.getMessage());
    }

    @Test
    public void testTimeLimit() throws InterruptedException {
        WorkbookAPIImpl workbookAPI = new WorkbookAPIImpl(workbook, 10, 1000, saveSize -> {}); // takes 300ms
        SheetImpl sheet = new SheetImpl(workbookAPI, workbook.createSheet("foo")); // takes 400ms
        sheet.addRow(Lists.newArrayList(new Value("v1")));
        TimeUnit.MILLISECONDS.sleep(1000);

        PseaLimitException e = assertThrows(PseaLimitException.class, () -> sheet.addRow(Lists.newArrayList(new Value("v2"))));
        assertThat(e.getMessage(), CoreMatchers.containsString("The Excel export reached a hard limit for 'time'"));
    }


}