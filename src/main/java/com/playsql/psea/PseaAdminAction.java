package com.playsql.psea;

/*
 * #%L
 * Play SQL Exports
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

import com.atlassian.confluence.core.ConfluenceActionSupport;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.playsql.psea.db.dao.PseaTaskDAO;
import com.playsql.psea.dto.DTOPseaTask;
import com.playsql.psea.impl.PseaServiceImpl;

import java.util.List;

public class PseaAdminAction extends ConfluenceActionSupport {
    private  PseaServiceImpl pseaService;
    private PseaTaskDAO pseaTaskDAO;
    private PermissionManager permissionManager;
    private Long timeLimit;
    private Long dataLimit;
    private Long rowLimit;

    @Override
    public boolean isPermitted() {
        return super.isPermitted()
                && permissionManager.isConfluenceAdministrator(AuthenticatedUserThreadLocal.get());
    }

    public String doAdmin() {
        rowLimit = pseaService.getRowLimit();
        timeLimit = pseaService.getTimeLimit();
        dataLimit = pseaService.getDataLimit();
        return INPUT;
    }

    public String doSave() {
        if (!isPermitted()) throw new IllegalStateException("Permissions haven't been checked properly.");
        pseaService.setDataLimit(dataLimit);
        pseaService.setRowLimit(rowLimit);
        pseaService.setTimeLimit(timeLimit);
        return INPUT;
    }

    public List<DTOPseaTask> getLastExportList() {
        return pseaTaskDAO.getList();
    }

    public void setPseaTaskDAO(PseaTaskDAO pseaTaskDAO) {
        this.pseaTaskDAO = pseaTaskDAO;
    }

    public void setPseaService(PseaServiceImpl pseaService) {
        this.pseaService = pseaService;
    }

    public Long getRowLimitDefault() {
        return PseaServiceImpl.MAX_ROWS_DEFAULT;
    }

    public Long getTimeLimitDefault() {
        return PseaServiceImpl.TIME_LIMIT_DEFAULT;
    }

    public Long getTimeLimitMax() {
        return PseaServiceImpl.TIME_LIMIT_MAX;
    }

    public Long getRowLimit() {
        return rowLimit;
    }

    public Long getTimeLimit() {
        return timeLimit;
    }

    public Long getDataLimit() {
        return dataLimit;
    }

    public Long getDataLimitDefault() {
        return PseaServiceImpl.DATA_LIMIT_DEFAULT;
    }

    public void setTimeLimit(Long timeLimit) {
        this.timeLimit = timeLimit;
    }

    public void setDataLimit(Long dataLimit) {
        this.dataLimit = dataLimit;
    }

    public void setRowLimit(Long rowLimit) {
        this.rowLimit = rowLimit;
    }

    @Override
    public void setPermissionManager(PermissionManager permissionManager) {
        this.permissionManager = permissionManager;
    }
}
