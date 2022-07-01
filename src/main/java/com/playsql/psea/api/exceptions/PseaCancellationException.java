package com.playsql.psea.api.exceptions;

import java.util.concurrent.CancellationException;

/**
 * Exception to throw when a job can't be started or continued because it
 * was cancelled, or because there are too many concurrent jobs.
 *
 * @since PSEA 1.7.1
 */
public class PseaCancellationException extends CancellationException {

    public PseaCancellationException() {
        // Default constructor
    }

    public PseaCancellationException(String message) {
        super(message);
    }
}
