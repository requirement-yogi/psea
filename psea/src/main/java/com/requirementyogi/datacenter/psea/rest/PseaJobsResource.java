package com.requirementyogi.datacenter.psea.rest;

import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.requirementyogi.datacenter.psea.db.dao.PseaTaskDAO;
import com.requirementyogi.datacenter.psea.db.entities.DBPseaTask;
import com.requirementyogi.datacenter.psea.dto.PseaTaskStatus;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static com.requirementyogi.datacenter.psea.dto.PseaTaskStatus.*;
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
        PseaTaskStatus status = of(job.getStatus());
        if (status != CANCELLING && status.isRunning()) {
            dao.save(job, CANCELLING, "Cancellation requested by " + user.getFullName());
            return ok("Cancellation requested").build();
        } else {
            dao.delete(job);
            return ok("Deleted").build();
        }
    }
}
