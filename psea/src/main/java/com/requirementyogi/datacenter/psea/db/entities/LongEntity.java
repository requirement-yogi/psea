package com.requirementyogi.datacenter.psea.db.entities;

import net.java.ao.RawEntity;
import net.java.ao.schema.AutoIncrement;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.PrimaryKey;

/**
 * An ActiveObjects table which has Long IDs as a primary key
 */
public interface LongEntity extends RawEntity<Long> {

    @AutoIncrement
    @NotNull
    @PrimaryKey("ID")
    long getID();

}
