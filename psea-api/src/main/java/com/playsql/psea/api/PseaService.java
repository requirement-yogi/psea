package com.playsql.psea.api;

import com.playsql.psea.api.exceptions.PseaCancellationException;

import java.io.File;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;

public interface PseaService {

    /**
     * Export excel file with default constraints
     *
     * The caller is responsible for deleting the file afterwards.
     * If not, it will only be deleted when the JVM is deleted.
     *
     * IMPORTANT: If you want the exception to be saved, no transaction should be started before
     * this export, AND any exception should be caught and returned to the UI as a simple error.
     *
     * @param f the consumer
     * @return File where the file is stored
     */
    File export(Consumer<WorkbookAPI> f) throws CancellationException;

    /**
     * Read an Excel file, and feed it to the consumer.
     *
     * @param file the file, either a physical file or an InputStream
     * @param consumer the consumer
     * @throws OutOfMemoryError if the underlying Apache POI throws an OOME. It is recommended to catch this exception in the calling method.
     * @throws PSEAImportException if the file is not in a readable format, or if an IOException was encountered
     */
    void extract(PseaInput file, ExcelImportConsumer consumer) throws OutOfMemoryError, PSEAImportException;

    /**
     * Monitors the 'callable' using a task in the PSEA database, so admins can check and cancel tasks.
     *
     * Beware, it will start a clean transaction, so any transactional content should have its own transaction.
     *
     * If there are already concurrent jobs (as verified in the database), and we reach the timeout,
     * it will refuse to start or continue, and throw a CancellationException. There is no need to
     * log or manage this CancellationException since it is already logged by PSEA.
     *
     * We couldn't have done is inside of pseaService.export() because backward-compatibility is required.
     *
     * @param taskDetails a map of String -> anything, containing the details of the task.
     *                    For PSEA 1.8.0, the only recognized key is 'details'.
     * @param callable the job to execute when the export is ready/possible
     * @param waitingTime if we are in a background task which can wait
     * @param transactionHasAlreadyStarted if true, that means AO (and probably the XWork action itself) has already
     *                                     started the transaction, making it impossible to save-and-flush the status
     *                                     of the task to the DB. If so, this method will start a thread, where the
     *                                     transaction is clean, and execute the 'callable' inside.
     * @throws com.playsql.psea.api.exceptions.PseaCancellationException if the task is cancelled,
     * @since PSEA 1.8.0
     */
    <T> T startMonitoredTask(
            Map<String, Object> taskDetails,
            Callable<T> callable,
            long waitingTime,
            boolean transactionHasAlreadyStarted
    ) throws PseaCancellationException;


    /**
     * Returns the version of PSEA
     */
    String getVersion();
}
