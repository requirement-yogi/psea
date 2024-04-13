package com.requirementyogi.datacenter.psea.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.confluence.api.model.accessmode.AccessMode;
import com.atlassian.confluence.api.service.accessmode.AccessModeService;
import com.atlassian.confluence.api.service.exceptions.ServiceException;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.google.common.collect.Maps;
import com.requirementyogi.datacenter.psea.db.dao.PseaTaskDAO;
import com.requirementyogi.datacenter.psea.db.entities.DBPseaTask;
import com.playsql.utils.compat.InternalBeanFactory;
import org.apache.commons.lang3.NotImplementedException;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class PseaTestUtils {

    public AccessModeService accessModeService = new AccessModeService() {
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

    public PluginSettingsFactory pluginSettingsFactory = new PluginSettingsFactory() {

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

    public final ActiveObjects ao = Mockito.mock(ActiveObjects.class);
    public final UserAccessor userAccessor = Mockito.mock(UserAccessor.class);
    public final PseaTaskDAO dao = new PseaTaskDAO(ao, userAccessor);
    public final DBPseaTask record = Mockito.mock(DBPseaTask.class);
    public final InternalBeanFactory internalBeanFActory = Mockito.mock(InternalBeanFactory.class);

    {
        long recordId = 1L;
        when(record.getID()).thenReturn(recordId);
        when(ao.executeInTransaction(any())).thenAnswer(invocation -> {
            TransactionCallback callback = invocation.getArgument(0);
            Object result = callback.doInTransaction();
            return result;
        });
        when(ao.get(eq(DBPseaTask.class), eq(recordId))).thenReturn(record);
        when(ao.create(any())).thenAnswer(invocation -> record);
    }

    public PseaServiceImpl psea = new PseaServiceImpl(pluginSettingsFactory, accessModeService, dao, ao, internalBeanFActory);

}
