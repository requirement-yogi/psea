package com.requirementyogi.datacenter.psea.web;

import com.atlassian.confluence.core.ConfluenceActionSupport;
import com.google.common.collect.Lists;
import java.util.LinkedList;
import java.util.ArrayList;
import com.requirementyogi.datacenter.psea.api.PseaService;
import com.requirementyogi.datacenter.psea.api.Sheet;
import com.requirementyogi.datacenter.psea.api.Value;

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
