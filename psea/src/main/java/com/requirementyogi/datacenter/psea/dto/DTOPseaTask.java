package com.requirementyogi.datacenter.psea.dto;

import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.sal.api.user.UserKey;
import com.requirementyogi.datacenter.psea.db.entities.DBPseaTask;
import com.requirementyogi.datacenter.psea.impl.PseaServiceImpl;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class DTOPseaTask {

    private static final SimpleDateFormat SIMPLE_DATE_FORMAT_HOUR_SECONDS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final Long id;
    private final String filename;
    private final Date startDate;
    private final Long duration;
    private final PseaTaskStatus status;
    private final String message;
    private final String userKey;
    private final String username;
    private final String userFullName;

    public DTOPseaTask(Long id,
                       String filename,
                       Date startDate,
                       Long duration,
                       PseaTaskStatus status,
                       String message,
                       String userKey,
                       String username,
                       String userFullName
    ) {
        this.id = id;
        this.filename = filename;
        this.startDate = startDate;
        this.duration = duration;
        this.status = status;
        this.message = message;
        this.userKey = userKey;
        this.username = username;
        this.userFullName = userFullName;
    }
    
    public static DTOPseaTask of(DBPseaTask dbTask, UserAccessor userAccessor) {
        String userKey = dbTask.getUserkey();
        String userFullName = null;
        String username = null;
        if (userKey != null) {
            @Nullable ConfluenceUser user = userAccessor.getExistingUserByKey(new UserKey(userKey));
            if (user != null) {
                username = user.getName();
                userFullName = user.getFullName();
            } else {
                username = userKey;
                userFullName = "Not found (" + userKey + ")";
            }
        }
        return new DTOPseaTask(
                dbTask.getID(),
                dbTask.getFilename(),
                dbTask.getStartdate(),
                dbTask.getDuration(),
                PseaTaskStatus.of(dbTask),
                dbTask.getMessage(),
                userKey,
                username,
                userFullName
        );
    }


    public String getFilename() {
        return filename;
    }

    public Date getStartDate() {
        return startDate;
    }

    public String getStartDateHuman() {
        if (startDate == null) return null;
        return SIMPLE_DATE_FORMAT_HOUR_SECONDS.format(startDate);
    }

    public Long getDuration() {
        return duration;
    }

    public String getDurationHuman() {
        long duration1 = duration != null ? duration : new Date().getTime() - startDate.getTime();
        if (duration1 < 1000)
            return duration1 + " ms";
        if (duration1 < 60000)
            return TimeUnit.MILLISECONDS.toSeconds(duration1) + " s";
        if (duration1 < PseaServiceImpl.TIME_LIMIT_MAX) {
            long minutes = TimeUnit.MILLISECONDS.toMinutes(duration1);
            long seconds = TimeUnit.MILLISECONDS.toSeconds(duration1 - TimeUnit.MINUTES.toMillis(minutes));
            return minutes + " min " + seconds + " s";
        }
        return "(End date not recorded)";
    }

    public PseaTaskStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public Long getId() {
        return id;
    }

    public String getUserKey() {
        return userKey;
    }

    public String getUsername() {
        return username;
    }

    public String getUserFullName() {
        return userFullName;
    }
}
