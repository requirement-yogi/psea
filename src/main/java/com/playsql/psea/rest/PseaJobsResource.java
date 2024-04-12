package com.playsql.psea.rest;

/*-
 * #%L
 * PSEA
 * %%
 * Copyright (C) 2016 - 2024 Requirement Yogi S.A.S.U.
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

import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.playsql.psea.db.dao.PseaTaskDAO;
import com.playsql.psea.db.entities.DBPseaTask;
import com.playsql.psea.dto.DTOPseaTask;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static com.playsql.psea.dto.DTOPseaTask.Status.*;
import static javax.ws.rs.core.Response.*;

@Path("/")
public class PseaJobsResource {

    private final PseaTaskDAO dao;
    private final PermissionManager permissionManager;

    @Inject
    public PseaJobsResource(PseaTaskDAO dao,
                            @ComponentImport PermissionManager permissionManager) {
        this.dao = dao;
        this.permissionManager = permissionManager;
    }

    @DELETE
    @Path("/jobs/{id}")
    public Response delete(@PathParam("id") Long id) {
        ConfluenceUser user = AuthenticatedUserThreadLocal.get();
        if (!permissionManager.isConfluenceAdministrator(user)) {
            return Response.status(Status.FORBIDDEN).entity("Only administrators are allowed to delete jobs").build();
        }
        DBPseaTask job = dao.get(id);
        if (job == null) {
            return Response.status(Status.NOT_FOUND).entity("No job with ID " + id).build();
        }
        DTOPseaTask.Status status = of(job.getStatus());
        if (status != CANCELLING && status.isRunning()) {
            dao.save(job, CANCELLING, "Cancellation requested by " + user.getFullName());
            return ok("Cancellation requested").build();
        } else {
            dao.delete(job);
            return ok("Deleted").build();
        }
    }
}
