package com.playsql.psea.api.exceptions;

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

import java.util.concurrent.CancellationException;

/**
 * Exception to throw when a job can't be started or continued because it
 * was cancelled, or because there are too many concurrent jobs.
 *
 * @since PSEA 1.8.0
 */
public class PseaCancellationException extends CancellationException {

    public PseaCancellationException() {
        // Default constructor
    }

    public PseaCancellationException(String message) {
        super(message);
    }
}
