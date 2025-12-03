package com.requirementyogi.datacenter.psea.compat;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.requirementyogi.datacenter.utils.activeobjects.AOContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@ExportAsService(AOContext.class)
public class PSEAAOContext extends AOContext {

    @Autowired
    public PSEAAOContext(@ComponentImport ActiveObjects ao) {
        super(ao, "AO_775078_", false);
    }
}
