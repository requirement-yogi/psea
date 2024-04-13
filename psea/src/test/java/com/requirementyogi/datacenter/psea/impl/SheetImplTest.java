package com.requirementyogi.datacenter.psea.impl;

import com.google.common.collect.Lists;
import com.requirementyogi.datacenter.psea.api.Value;
import com.requirementyogi.datacenter.psea.dto.PseaLimitException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SheetImplTest {

    private XSSFWorkbook workbook;

    @BeforeEach
    public void setUp() {
        workbook = new XSSFWorkbook();
    }

    @AfterEach
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
