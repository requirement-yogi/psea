package com.playsql.psea.db.entities;

/*-
 * #%L
 * PSEA
 * %%
 * Copyright (C) 2016 - 2024 Requirement Yogi S.A.S.U.
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
