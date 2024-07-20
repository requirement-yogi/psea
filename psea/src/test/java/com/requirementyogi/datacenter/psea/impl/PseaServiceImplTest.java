package com.requirementyogi.datacenter.psea.impl;

import com.google.common.collect.Lists;
import com.requirementyogi.datacenter.psea.api.Sheet;
import com.requirementyogi.datacenter.psea.api.Value;
import com.requirementyogi.datacenter.psea.dto.PseaLimitException;
import com.requirementyogi.datacenter.psea.dto.PseaTaskStatus;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

public class PseaServiceImplTest {

    private final PseaTestUtils utils = new PseaTestUtils();

    @Test
    public void testSizeLimit() {
        utils.psea.setDataLimit(300L);
        try {
            utils.psea.export(workbook -> {
                Sheet sheet = workbook.newSheet("foo");
                sheet.addRow(Lists.newArrayList(new Value("v1"))); // Takes 10 for the row, 10 for the cell, 2 for the contents
                sheet.addRow(Lists.newArrayList(new Value(StringUtils.repeat("a", 250)))); // Still under 300

                PseaLimitException e = assertThrows(PseaLimitException.class,
                        () -> sheet.addRow(Lists.newArrayList(new Value(StringUtils.repeat("a", 250)))));
                throw e; // We need to let the exception bubble up
            });
        } catch (PseaLimitException e) {
            assertThat(e.getMessage(), CoreMatchers.containsString("The Excel export reached a hard limit for 'size'"));
        }

        verify(utils.record, atLeastOnce()).setStatus(PseaTaskStatus.ERROR.getDbValue());
    }
}
