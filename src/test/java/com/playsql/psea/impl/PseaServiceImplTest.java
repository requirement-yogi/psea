package com.playsql.psea.impl;

import com.google.common.collect.Lists;
import com.playsql.psea.api.Sheet;
import com.playsql.psea.api.Value;
import com.playsql.psea.db.entities.DBPseaTask;
import com.playsql.psea.dto.PseaLimitException;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static com.playsql.psea.impl.PseaTestUtils.ACCESS_MODE_SERVICE;
import static com.playsql.psea.impl.PseaTestUtils.PSEA;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;

public class PseaServiceImplTest {

    @Test
    public void testSizeLimit() {
        PseaServiceImpl psea = PseaTestUtils.PSEA;
        psea.setDataLimit(300L);
        try {
            psea.export(workbook -> {
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

        verify(PseaTestUtils.RECORD).setStatus(DBPseaTask.STATUS_ERROR);

    }
}