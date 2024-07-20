package com.requirementyogi.datacenter.psea.db.dao;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;

import java.util.ArrayList;
import com.requirementyogi.datacenter.psea.api.exceptions.PseaCancellationException;
import com.requirementyogi.datacenter.psea.db.entities.DBPseaTask;
import com.requirementyogi.datacenter.psea.dto.DTOPseaTask;
import com.requirementyogi.datacenter.psea.dto.PseaTaskStatus;
import net.java.ao.Query;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class PseaTaskDAO {

    /** Maximum size of the logs */
    public static int ITEMS_IN_UI = 300;
    private static int KEEP_ITEMS = 5000;
    private final ActiveObjects ao;
    private final UserAccessor userAccessor;

    public PseaTaskDAO(@ComponentImport ActiveObjects ao,
                       @ComponentImport UserAccessor userAccessor) {
        this.ao = ao;
        this.userAccessor = userAccessor;
    }
    
    public DBPseaTask create(@Nonnull PseaTaskStatus status,
                             @Nullable ConfluenceUser confluenceUser,
                             @Nullable String details
    ) {
        return ao.executeInTransaction(() -> {
            clearOldData();
            DBPseaTask task = ao.create(DBPseaTask.class);
            task.setFilename("File not written to disk");
            task.setUserkey(confluenceUser != null ? confluenceUser.getKey().getStringValue() : null);
            task.setDetails(details);
            task.setStartdate(new Date());
            task.setStatus(status.getDbValue());
            task.save();
            return task;
        });
    }

    public DBPseaTask get(Long id) {
        if (id == null) return null;
        return ao.executeInTransaction(() -> {
            return ao.get(DBPseaTask.class, id);
        });
    }

    /** Saves the status of the task, after various checks:
     * - Check there is a record (there is none during READ_ONLY mode),
     * - Appends the message instead of setting it,
     * - If the task is done, set the duration.
     */
    public void save(DBPseaTask record, @Nonnull PseaTaskStatus status, String message) {
        if (record == null) return;
        ao.executeInTransaction(() -> {
            if (message != null) {
                String existingMessage = record.getMessage();
                if (existingMessage != null) {
                    record.setMessage(StringUtils.abbreviate(existingMessage + " \n" + message, 600));
                } else {
                    record.setMessage(StringUtils.abbreviate(message, 600));
                }
            }
            // We only save the status if it isn't 'ERROR' yet (so no transition from ERROR -> DONE),
            // and we set the duration.
            if (!Objects.equals(record.getStatus(), PseaTaskStatus.ERROR.getDbValue()) && !Objects.equals(record.getStatus(), PseaTaskStatus.CANCELLED.getDbValue())) {
                record.setStatus(status.getDbValue());
                if (status == PseaTaskStatus.DONE || status == PseaTaskStatus.ERROR || status == PseaTaskStatus.CANCELLED) {
                    Date startdate = record.getStartdate();
                    if (startdate == null) {
                        record.setDuration(0);
                    } else {
                        record.setDuration(new Date().getTime() - startdate.getTime());
                    }
                }
            }
            record.save();
            return null;
        });
    }

    public void save(long id, @Nonnull PseaTaskStatus status, String message) {
        ao.executeInTransaction(() -> {
            DBPseaTask task = ao.get(DBPseaTask.class, id);
            if (task != null) {
                save(task, status, message);
            }
            return null;
        });
    }

    /**
     * Save the provided record. Also, check the record from the DB, and if it's 'cancelling', then throw an
     * exception.
     *
     * If the record is non-existing in the DB, then that means an administrator has deleted it. Cancel the
     * task immediately.
     */
    public void saveAndCheckCancellation(DBPseaTask record) {
        boolean cancelled = ao.executeInTransaction(() -> {
            DBPseaTask freshRecord = ao.get(DBPseaTask.class, record.getID());
            if (freshRecord == null) {
                return true;
            }
            PseaTaskStatus status = PseaTaskStatus.of(freshRecord);
            if (status == PseaTaskStatus.CANCELLING || status == PseaTaskStatus.CANCELLED) {
                record.setStatus(PseaTaskStatus.CANCELLED.getDbValue());
                record.save();
                return true;
            }
            record.save();
            return false;
        });
        if (cancelled) {
            throw new PseaCancellationException("The task was marked as cancelled.");
        }
    }

    /** Delete data above 200 items */
    public void clearOldData() {
        int count = ao.count(DBPseaTask.class);
        if (count > KEEP_ITEMS) {
            int itemsToRemove = Math.min(count - KEEP_ITEMS, 2000);
            DBPseaTask[] items = ao.find(DBPseaTask.class, Query.select().order("STARTDATE ASC").limit(itemsToRemove));
            for (DBPseaTask dbTask : items) {
                ao.delete(dbTask);
            }
        }
    }

    /**
     * Return the last 200 saves.
     */
    public List<DTOPseaTask> getList(int limit) {
        List<DBPseaTask> list = new ArrayList<>();
        fetchItems(list, PseaTaskStatus.NOT_STARTED, limit);
        fetchItems(list, PseaTaskStatus.PREPARING, limit);
        fetchItems(list, PseaTaskStatus.CANCELLING, limit);
        fetchItems(list, PseaTaskStatus.IN_PROGRESS, limit);
        fetchItemsNotIn(list, limit, PseaTaskStatus.NOT_STARTED, PseaTaskStatus.PREPARING, PseaTaskStatus.IN_PROGRESS, PseaTaskStatus.CANCELLING);
        return list.stream().map((DBPseaTask dbTask) -> DTOPseaTask.of(dbTask, userAccessor)).collect(Collectors.toList());
    }

    private void fetchItems(List<DBPseaTask> list, PseaTaskStatus status, int limit) {
        if (limit < list.size()) return;
        DBPseaTask[] items = ao.find(DBPseaTask.class, Query.select().where("STATUS = ?", status.getDbValue()).order("STARTDATE DESC").limit(limit - list.size()));
        Collections.addAll(list, items);
    }

    private void fetchItemsNotIn(List<DBPseaTask> list, int limit, PseaTaskStatus... status) {
        if (limit < list.size()) return;
        DBPseaTask[] items = ao.find(DBPseaTask.class, Query.select().where(
                "STATUS NOT IN (" + StringUtils.repeat("?", ", ", status.length) + ")", (Object[]) status
        ).order("STARTDATE DESC").limit(limit - list.size()));
        Collections.addAll(list, items);
    }

    /** Returns the number of jobs that have a status where {@link PseaTaskStatus#isRunning()} is true */
    public int countRunningJobs() {
        return ao.executeInTransaction(() -> {
            List<PseaTaskStatus> statuses = PseaTaskStatus.listOfRunningStatuses();
            return ao.count(DBPseaTask.class,
                    "STATUS IN (" + StringUtils.repeat("?", ", ", statuses.size()) + ")",
                    statuses.stream().map(PseaTaskStatus::getDbValue).toArray());
        });
    }

    public void delete(DBPseaTask job) {
        ao.executeInTransaction(() -> {
            ao.delete(job);
            return null;
        });
    }
}
