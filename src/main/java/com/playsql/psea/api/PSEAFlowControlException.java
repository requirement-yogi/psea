package com.playsql.psea.api;

/*
 * #%L
 * Play SQL Exports
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

/**
 * A FlowControlException is an exception that doesn't fill the stacktrace, hence can be used for flow control.
 */
public class PSEAFlowControlException extends RuntimeException {
    public PSEAFlowControlException() {
    }

    public PSEAFlowControlException(String message) {
        super(message);
    }

    public PSEAFlowControlException(String message, Throwable cause) {
        super(message, cause);
    }

    public PSEAFlowControlException(Throwable cause) {
        super(cause);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        // Do not fill the stacktrace, because that's what takes time.
        return this;
    }
}
