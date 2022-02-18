package com.playsql.psea.db.entities;

import net.java.ao.Preload;
import net.java.ao.schema.Table;

import java.util.Date;

/**
 * List of recently exported files.
 *
 * It tracks what size the files are, and whether they have crashed.
 */
@Preload
@Table("PSEATASK")
public interface DBPseaTask extends LongEntity {

    String STATUS_IN_PROGRESS = "IN_PROGRESS";
    String STATUS_WRITING = "WRITING";
    String STATUS_DONE = "DONE";
    String STATUS_ERROR = "ERROR";

    String getFilename();
    void setFilename(String filename);

    /**
     * When the export started.
     *
     * Exports older than 30 days are deleted.
     * */
    Date getStartdate();
    void setStartdate(Date date);

    /** Duration of the task, in milliseconds */
    Long getDuration();
    void setDuration(long duration);

    /** The size. The unit is arbitrary (mainly bytes of data before the file is built) */
    Long getSize();
    void setSize(long size);

    /** The status of the export */
    String getStatus();
    void setStatus(String status);

    /** The (error) message, especially when the status is ERROR */
    String getMessage();
    void setMessage(String status);
}
