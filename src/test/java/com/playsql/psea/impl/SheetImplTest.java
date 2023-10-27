package com.playsql.psea.impl;

/*-
 * #%L
 * PSEA
 * %%
 * Copyright (C) 2016 - 2023 Requirement Yogi S.A.S.U.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.google.common.collect.Lists;
import com.playsql.psea.api.Value;
import com.playsql.psea.dto.PseaLimitException;
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
