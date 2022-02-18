package com.playsql.psea.impl;

import com.atlassian.confluence.api.model.accessmode.AccessMode;
import com.atlassian.confluence.api.service.accessmode.AccessModeService;
import com.atlassian.confluence.api.service.exceptions.ServiceException;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.google.common.collect.Maps;
import com.playsql.psea.db.dao.PseaTaskDAO;
import com.playsql.psea.db.entities.DBPseaTask;
import org.apache.commons.lang3.NotImplementedException;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;

import static org.mockito.Mockito.when;

public class PseaTestUtils {

    public static AccessModeService ACCESS_MODE_SERVICE = new AccessModeService() {
        @Override
        public AccessMode getAccessMode() {
            return AccessMode.READ_WRITE;
        }

        @Override
        public void updateAccessMode(AccessMode accessMode) throws ServiceException {
            throw new NotImplementedException("updateAccessMode");
        }

        @Override
        public boolean isReadOnlyAccessModeEnabled() {
            return false;
        }

        @Override
        public boolean shouldEnforceReadOnlyAccess() {
            return false;
        }

        @Override
        public <T> T withReadOnlyAccessExemption(Callable<T> callable) throws ServiceException {
            throw new NotImplementedException("updateAccessMode");
        }
    };

    public static PluginSettingsFactory PLUGIN_SETTINGS = new PluginSettingsFactory() {

        private Map<String, String> storage = Maps.newHashMap();

        @Override
        public PluginSettings createSettingsForKey(String s) {
            throw new NotImplementedException("createSettingsForKey");
        }

        @Override
        public PluginSettings createGlobalSettings() {
            return new PluginSettings() {

                @Override
                public Object get(String key) {
                    return storage.get(key);
                }

                @Override
                public Object put(String key, Object value) {
                    return storage.put(key, Objects.toString(value));
                }

                @Override
                public Object remove(String key) {
                    return storage.remove(key);
                }
            };
        }
    };

    public static final PseaTaskDAO DAO = Mockito.mock(PseaTaskDAO.class);
    public static final DBPseaTask RECORD = Mockito.mock(DBPseaTask.class);

    static {
        when(DAO.create()).thenReturn(RECORD);
    }


    public static PseaServiceImpl PSEA = new PseaServiceImpl(PLUGIN_SETTINGS, ACCESS_MODE_SERVICE, DAO);

}
