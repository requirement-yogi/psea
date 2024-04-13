package com.playsql.psea.web;

import com.atlassian.confluence.core.ConfluenceActionSupport;
import com.google.common.collect.Lists;
import com.playsql.psea.api.PseaService;
import com.playsql.psea.api.Sheet;
import com.playsql.psea.api.Value;

public class AboutAction extends ConfluenceActionSupport {
    public PseaService pseaService;

    public String doTest() {
        pseaService.export(workbookAPI -> {
            Sheet sh = workbookAPI.newSheet("Test");
            sh.addRow(Lists.newArrayList(new Value("Test")));
        });
        return SUCCESS;
    }

    public void setPseaService(PseaService pseaService) {
        this.pseaService = pseaService;
    }
}
