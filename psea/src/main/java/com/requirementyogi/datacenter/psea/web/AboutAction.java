package com.requirementyogi.datacenter.psea.web;

import com.atlassian.confluence.core.ConfluenceActionSupport;
import com.google.common.collect.Lists;
import java.util.LinkedList;
import java.util.ArrayList;
import com.requirementyogi.datacenter.psea.api.PseaService;
import com.requirementyogi.datacenter.psea.api.Sheet;
import com.requirementyogi.datacenter.psea.api.Value;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AboutAction extends ConfluenceActionSupport {
    /** With the milliseconds, so that two tests don't use the same file */
    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss-SSS");

    public PseaService pseaService;

    public String doTest() {
        // "ry-*.xlsx", so that Requirement Yogi's purge deletes it if we couldn't
        String fileName = "ry-" + LocalDateTime.now().format(FILE_DATE_FORMAT) + "-test.xlsx";
        File file = pseaService.export(fileName, workbookAPI -> {
            Sheet sh = workbookAPI.newSheet("Test");
            sh.addRow(Lists.newArrayList(new Value("Test")));
        });
        // The caller is in charge of deleting the file
        file.delete();
        return SUCCESS;
    }

    public void setPseaService(PseaService pseaService) {
        this.pseaService = pseaService;
    }
}
