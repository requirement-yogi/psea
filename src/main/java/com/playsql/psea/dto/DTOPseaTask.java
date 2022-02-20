package com.playsql.psea.dto;

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

import com.playsql.psea.db.entities.DBPseaTask;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class DTOPseaTask {

    private static final SimpleDateFormat SIMPLE_DATE_FORMAT_HOUR_SECONDS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    public enum Status {

        IN_PROGRESS,
        WRITING,
        DONE,
        ERROR;

        public static Status of(DBPseaTask dbTask) {
            return Arrays.stream(Status.values()).filter(status -> Objects.equals(status.name(), dbTask.getStatus())).findFirst().orElse(null);
        }
    };
    
    private final String filename;
    private final Date startDate;
    private final Long duration;
    private final Status status;
    private final String message;

    public DTOPseaTask(String filename, Date startDate, Long duration, Status status, String message) {
        this.filename = filename;
        this.startDate = startDate;
        this.duration = duration;
        this.status = status;
        this.message = message;
    }
    
    public static DTOPseaTask of(DBPseaTask dbTask) {
        return new DTOPseaTask(
                dbTask.getFilename(),
                dbTask.getStartdate(),
                dbTask.getDuration(),
                Status.of(dbTask),
                dbTask.getMessage());
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
            return duration1 + "ms";
        if (duration1 < 60000)
            return TimeUnit.MILLISECONDS.toSeconds(duration1) + " s";
        if (duration1 < TimeUnit.HOURS.toMillis(1)) {
            long minutes = TimeUnit.MILLISECONDS.toMinutes(duration1);
            long seconds = TimeUnit.MILLISECONDS.toSeconds(duration1 - TimeUnit.MINUTES.toMillis(minutes));
            return minutes + " min " + seconds + " s";
        }
        return "(End date not recorded)";
    }

    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
