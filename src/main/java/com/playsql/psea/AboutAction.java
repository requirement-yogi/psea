package com.playsql.psea;

/*
 * #%L
 * Play SQL Exports
 * %%
 * Copyright (C) 2016 Play SQL S.A.S.U.
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

import com.atlassian.confluence.core.ConfluenceActionSupport;
import com.google.common.collect.Lists;
import com.playsql.psea.api.PseaService;
import com.playsql.psea.api.Sheet;
import com.playsql.psea.api.Value;
import com.playsql.psea.api.WorkbookAPI;

import java.util.function.Consumer;

public class AboutAction extends ConfluenceActionSupport {
    public PseaService pseaService;

    public String doTest() {
        pseaService.export(workbookAPI -> {
            Sheet sh = workbookAPI.newSheet("Blanket");
            sh.addRow(Lists.newArrayList(new Value("Jail")));
        });
        return SUCCESS;
    }

    public void setPseaService(PseaService pseaService) {
        this.pseaService = pseaService;
    }
}
