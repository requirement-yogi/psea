package com.playsql.psea.api;

/*
 * #%L
 * Play SQL Exports
 * %%
 * Copyright (C) 2016 - 2023 Requirement Yogi S.A.S.U.
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

import com.playsql.psea.api.PseaService.PseaInput;

/**
 * A checked exception, for all situations where an import fails (most often when Apache POI fails)
 */
public class PSEAImportException extends Exception {
    public PSEAImportException() {
    }

    public PSEAImportException(String message) {
        super(message);
    }

    public PSEAImportException(String message, Throwable cause) {
        super(message, cause);
    }

    public PSEAImportException(Throwable cause) {
        super(cause);
    }

    public PSEAImportException(PseaInput workbookFile, Throwable e) {
        super("Exception while importing " + workbookFile.getFileName(), e);
    }
}
