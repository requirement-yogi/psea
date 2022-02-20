package com.playsql.psea.impl;

/*-
 * #%L
 * PSEA
 * %%
 * Copyright (C) 2016 - 2022 Requirement Yogi S.A.S.U.
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
import com.playsql.psea.api.Sheet;
import com.playsql.psea.api.Value;
import com.playsql.psea.db.entities.DBPseaTask;
import com.playsql.psea.dto.PseaLimitException;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
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

        verify(utils.record, atLeastOnce()).setStatus(DBPseaTask.STATUS_ERROR);

    }
}
