package com.playsql.psea.db.dao;

/*-
 * #%L
 * PSEA
 * %%
 * Copyright (C) 2016 - 2023 Requirement Yogi S.A.S.U.
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
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.confluence.user.UserAccessor;
import com.google.common.collect.Lists;
import com.playsql.psea.api.exceptions.PseaCancellationException;
import com.playsql.psea.db.entities.DBPseaTask;
import com.playsql.psea.dto.DTOPseaTask;
import com.playsql.psea.dto.DTOPseaTask.Status;
import com.playsql.psea.dto.PseaLimitException;
import net.java.ao.Query;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class PseaTaskDAO {

    /** Maximum size of the logs */
    public static int ITEMS_IN_UI = 300;
    private static int KEEP_ITEMS = 5000;
    private final ActiveObjects ao;
    private final UserAccessor userAccessor;

    public PseaTaskDAO(ActiveObjects ao, UserAccessor userAccessor) {
        this.ao = ao;
        this.userAccessor = userAccessor;
    }
    
    public DBPseaTask create(@Nonnull Status status,
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
    public void save(DBPseaTask record, @Nonnull Status status, String message) {
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
            if (!Objects.equals(record.getStatus(), Status.ERROR.getDbValue()) && !Objects.equals(record.getStatus(), Status.CANCELLED.getDbValue())) {
                record.setStatus(status.getDbValue());
                if (status == Status.DONE || status == Status.ERROR || status == Status.CANCELLED) {
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

    public void save(long id, @Nonnull Status status, String message) {
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
            Status status = Status.of(freshRecord);
            if (status == Status.CANCELLING || status == Status.CANCELLED) {
                record.setStatus(DTOPseaTask.Status.CANCELLED.getDbValue());
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
        List<DBPseaTask> list = Lists.newArrayList();
        fetchItems(list, Status.NOT_STARTED, limit);
        fetchItems(list, Status.PREPARING, limit);
        fetchItems(list, Status.CANCELLING, limit);
        fetchItems(list, Status.IN_PROGRESS, limit);
        fetchItemsNotIn(list, limit, Status.NOT_STARTED, Status.PREPARING, Status.IN_PROGRESS, Status.CANCELLING);
        return list.stream().map((DBPseaTask dbTask) -> DTOPseaTask.of(dbTask, userAccessor)).collect(Collectors.toList());
    }

    private void fetchItems(List<DBPseaTask> list, Status status, int limit) {
        if (limit < list.size()) return;
        DBPseaTask[] items = ao.find(DBPseaTask.class, Query.select().where("STATUS = ?", status.getDbValue()).order("STARTDATE DESC").limit(limit - list.size()));
        Collections.addAll(list, items);
    }

    private void fetchItemsNotIn(List<DBPseaTask> list, int limit, Status... status) {
        if (limit < list.size()) return;
        DBPseaTask[] items = ao.find(DBPseaTask.class, Query.select().where(
                "STATUS NOT IN (" + StringUtils.repeat("?", ", ", status.length) + ")", (Object[]) status
        ).order("STARTDATE DESC").limit(limit - list.size()));
        Collections.addAll(list, items);
    }

    /** Returns the number of jobs that have a status where {@link Status#isRunning()} is true */
    public int countRunningJobs() {
        return ao.executeInTransaction(() -> {
            List<Status> statuses = Status.listOfRunningStatuses();
            return ao.count(DBPseaTask.class,
                    "STATUS IN (" + StringUtils.repeat("?", ", ", statuses.size()) + ")",
                    statuses.stream().map(Status::getDbValue).toArray());
        });
    }

    public void delete(DBPseaTask job) {
        ao.executeInTransaction(() -> {
            ao.delete(job);
            return null;
        });
    }
}
