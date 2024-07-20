package com.requirementyogi.datacenter.psea.dto;

import com.requirementyogi.datacenter.psea.db.entities.DBPseaTask;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public enum PseaTaskStatus {

    NOT_STARTED(false, false, "NOT_STARTED"),
    PREPARING(true, false, "PREPARING"),
    IN_PROGRESS(true, false, "IN_PROGRESS"),
    WRITING(true, false, "WRITING"),

    DONE(false, true, "DONE"),
    ERROR(false, true, "ERROR"),

    CANCELLING(true, false, "CANCELLING"),
    CANCELLED(false, true, "CANCELLED");

    /**
     * True if a thread is currently running for this task,
     * False if the process is not started, finished or almost finished.
     */
    private final boolean running;

    /**
     * True if a status is a final state, i.e. if the thread
     * is finished or almost finished.
     */
    private final boolean finalState;

    /**
     * String for the value in the database
     */
    private final String dbValue;

    PseaTaskStatus(boolean running, boolean finalState, String dbValue) {
        this.running = running;
        this.finalState = finalState;
        this.dbValue = dbValue;
    }

    public static PseaTaskStatus of(DBPseaTask dbTask) {
        return of(dbTask.getStatus());
    }

    public static PseaTaskStatus of(String status) {
        return Arrays.stream(PseaTaskStatus.values())
                     .filter(value -> Objects.equals(value.getDbValue(), status))
                     .findFirst()
                     .orElse(null);
    }

    /**
     * Returns the list of 'running' statuses
     */
    public static List<PseaTaskStatus> listOfRunningStatuses() {
        return Arrays.stream(values()).filter(PseaTaskStatus::isRunning).collect(Collectors.toList());
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isFinalState() {
        return finalState;
    }

    public String getDbValue() {
        return dbValue;
    }
}
