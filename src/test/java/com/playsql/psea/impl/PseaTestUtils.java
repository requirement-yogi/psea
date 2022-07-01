package com.playsql.psea.impl;

/*-
 * #%L
 * PSEA
 * %%
 * Copyright (C) 2016 - 2022 Requirement Yogi S.A.S.U.
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

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.confluence.api.model.accessmode.AccessMode;
import com.atlassian.confluence.api.service.accessmode.AccessModeService;
import com.atlassian.confluence.api.service.exceptions.ServiceException;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.google.common.collect.Maps;
import com.playsql.psea.db.dao.PseaTaskDAO;
import com.playsql.psea.db.entities.DBPseaTask;
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

    public PseaServiceImpl psea = new PseaServiceImpl(pluginSettingsFactory, accessModeService, dao);

}
