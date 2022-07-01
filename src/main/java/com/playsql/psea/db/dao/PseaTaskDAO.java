package com.playsql.psea.db.dao;

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
import com.google.common.collect.Lists;
import com.playsql.psea.db.entities.DBPseaTask;
import com.playsql.psea.dto.DTOPseaTask;
import com.playsql.psea.dto.DTOPseaTask.Status;
import net.java.ao.Query;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PseaTaskDAO {

    /** Maximum size of the logs */
    public static int ITEMS_IN_UI = 300;
    private static int KEEP_ITEMS = 5000;
    private final ActiveObjects ao;

    public PseaTaskDAO(ActiveObjects ao) {
        this.ao = ao;
    }
    
    public DBPseaTask create(@Nonnull Status status) {
        return ao.executeInTransaction(() -> {
            clearOldData();
            DBPseaTask task = ao.create(DBPseaTask.class);
            task.setFilename("File not written to disk");
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
            if (!Objects.equals(record.getStatus(), Status.ERROR.getDbValue())) {
                record.setStatus(status.getDbValue());
                if (status == Status.DONE || status == Status.ERROR) {
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

    public void save(DBPseaTask record) {
        ao.executeInTransaction(() -> {
            record.save();
            return null;
        });
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
        fetchItems(list, Status.IN_PROGRESS, limit);
        fetchItems(list, Status.CANCELLING, limit);
        fetchItemsNotIn(list, limit, Status.NOT_STARTED, Status.PREPARING, Status.IN_PROGRESS, Status.CANCELLING);
        return list.stream().map(DTOPseaTask::of).collect(Collectors.toList());
    }

    private void fetchItems(List<DBPseaTask> list, Status status, int limit) {
        if (limit < list.size()) return;
        DBPseaTask[] items = ao.find(DBPseaTask.class, Query.select().where("STATUS = ?", status.getDbValue()).order("STARTDATE DESC").limit(limit - list.size()));
        Collections.addAll(list, items);
    }

    private void fetchItemsNotIn(List<DBPseaTask> list, int limit, Status... status) {
        if (limit < list.size()) return;
        DBPseaTask[] items = ao.find(DBPseaTask.class, Query.select().where(
                "STATUS NOT IN (" + StringUtils.repeat("?", ", ", status.length) + ")", status
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
