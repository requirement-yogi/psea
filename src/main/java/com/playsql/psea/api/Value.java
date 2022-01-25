package com.playsql.psea.api;

/*
 * #%L
 * Play SQL Exports
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

import java.util.Objects;

public class Value {
    private final WorkbookAPI.Style format;
    private final String value;
    private final String href;

    public Value(Object value) {
        this.value = Objects.toString(value);
        this.format = null;
        this.href = null;
    }

    public Value(WorkbookAPI.Style format, Object value) {
        this.format = format;
        this.value = Objects.toString(value);
        href = null;
    }

    public Value(WorkbookAPI.Style format, Object value, String href) {
        this.format = format;
        this.value = Objects.toString(value);
        this.href = href;
    }

    public WorkbookAPI.Style getFormat() {
        return format;
    }

    public String getValue() {
        return value;
    }

    public String getHref() {
        return href;
    }

    public String toString() {
        return String.format("%s (%s, %s)",
            value,
            Objects.toString(format),
            href);
    }
}
