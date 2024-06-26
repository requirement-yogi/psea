package com.requirementyogi.datacenter.psea.db.entities;

import net.java.ao.Preload;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

import java.util.Date;

import static net.java.ao.schema.StringLength.UNLIMITED;

/**
 * List of recently exported files.
 *
 * It tracks what size the files are, and whether they have crashed.
 */
@Preload
@Table("PSEATASK")
public interface DBPseaTask extends LongEntity {

    /** User who performed the export */
    String getUserkey();
    void setUserkey(String userkey);

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

    /**
     * The details of the export, i.e. the parameters, or even the URL if possible.
     */
    String getDetails();
    @StringLength(UNLIMITED)
    void setDetails(String details);
}
