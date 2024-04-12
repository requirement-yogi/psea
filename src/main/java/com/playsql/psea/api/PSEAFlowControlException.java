package com.playsql.psea.api;

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
