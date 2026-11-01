package com.requirementyogi.datacenter.psea.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.confluence.api.model.accessmode.AccessMode;
import com.atlassian.confluence.api.service.accessmode.AccessModeService;
import com.atlassian.confluence.api.service.exceptions.ServiceException;
import com.atlassian.confluence.setup.BootstrapManager;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.google.common.collect.Maps;
import com.requirementyogi.datacenter.psea.db.dao.PseaTaskDAO;
import com.requirementyogi.datacenter.psea.db.entities.DBPseaTask;
import com.requirementyogi.datacenter.utils.compat.InternalBeanFactory;
import org.apache.commons.lang3.NotImplementedException;
import org.mockito.Mockito;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class PseaTestUtils {

    /** With the milliseconds, so that two tests don't use the same file */
    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss-SSS");

    /**
     * "ry-[date]-[description].xlsx", like the files of Requirement Yogi, so that its purge deletes them.
     */
    public static String buildFileName(String description) {
        return "ry-" + LocalDateTime.now().format(FILE_DATE_FORMAT) + "-" + description + ".xlsx";
    }

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
    public final PseaTaskDAO dao = new PseaTaskDAO(ao, null);
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

    /** The local home: The files are created in "target/psea-test-home/temp/yogi" */
    public final ApplicationProperties applicationProperties = Mockito.mock(ApplicationProperties.class);
    /** No temporary directory is configured in confluence.cfg.xml (getFilePathProperty() returns null) */
    public final BootstrapManager bootstrapManager = Mockito.mock(BootstrapManager.class);

    {
        when(applicationProperties.getLocalHomeDirectory())
                .thenReturn(Optional.of(Paths.get("target", "psea-test-home").toAbsolutePath()));
    }

    public PseaServiceImpl psea = new PseaServiceImpl(
            pluginSettingsFactory,
            applicationProperties,
            bootstrapManager,
            accessModeService,
            dao,
            ao,
            internalBeanFActory
    );

}
