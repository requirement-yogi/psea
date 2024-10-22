package com.requirementyogi.datacenter.psea.managers;

import com.requirementyogi.datacenter.utils.compat.PluginDescriptor;
import org.springframework.stereotype.Component;

@Component
public class PSEAPluginDescriptor implements PluginDescriptor {

    @Override
    public String getAOTablePrefix() {
        return "AO_775078_";
    }

}
