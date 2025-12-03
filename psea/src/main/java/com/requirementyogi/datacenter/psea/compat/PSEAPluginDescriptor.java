package com.requirementyogi.datacenter.psea.compat;

import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.requirementyogi.datacenter.utils.PluginUtil;
import com.requirementyogi.datacenter.utils.compat.PluginDescriptor;
import com.requirementyogi.datacenter.utils.confluence.compat.AbstractConfluencePluginDescriptor;
import com.requirementyogi.datacenter.utils.confluence.compat.CompatibilityLayer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@ExportAsService(PluginDescriptor.class)
public class PSEAPluginDescriptor extends AbstractConfluencePluginDescriptor {

    private final PermissionManager permissionManager;

    @Autowired
    public PSEAPluginDescriptor(
            PluginAccessor pluginAccessor,
            UserAccessor userAccessor,
            CompatibilityLayer compatibilityLayer,
            PermissionManager permissionManager
    ) {
        super(pluginAccessor, userAccessor, compatibilityLayer);
        this.permissionManager = permissionManager;
    }

    @Override
    public String getPluginName() {
        return "PSEA - Export Add-on";
    }

    @Override
    public String getPluginKey() {
        return PluginUtil.PLUGIN_PSEA_KEY;
    }

    @Override
    public String getPluginLetters() {
        return "psea";
    }

    @Override
    public String getRestBaseUrl() {
        return "/rest/psea/1";
    }

    @Override
    public boolean isAdmin() {
        ConfluenceUser user = AuthenticatedUserThreadLocal.get();
        return permissionManager.isConfluenceAdministrator(user);
    }

    @Override
    public String getLoggedInUserKey() {
        ConfluenceUser user = AuthenticatedUserThreadLocal.get();
        return user != null ? user.getKey().getStringValue() : null;
    }
}
