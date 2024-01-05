package com.playsql.psea.dto;

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

public class PseaLimitException extends RuntimeException {

    /** The limit not to cross */
    private final long limit;

    /** The cross */
    private final long currentSize;

    private final String limitName;
    private final String unit;

    public PseaLimitException(long currentSize, long limit, String limitName, String unit) {
        super(
                "The Excel export reached a hard limit for '" + limitName + "': "
                        + currentSize + " " + unit + " instead of " + limit + " " + unit
        );
        this.limit = limit;
        this.currentSize = currentSize;
        this.limitName = limitName;
        this.unit = unit;
    }

}
