package com.requirementyogi.datacenter.psea.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.confluence.api.model.accessmode.AccessMode;
import com.atlassian.confluence.api.service.accessmode.AccessModeService;
import com.atlassian.confluence.setup.BootstrapManager;
import com.atlassian.confluence.setup.ConfluenceBootstrapConstants;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.requirementyogi.datacenter.utils.PluginUtil;
import com.requirementyogi.datacenter.utils.compat.InternalBeanFactory;
import com.requirementyogi.datacenter.psea.api.*;
import com.requirementyogi.datacenter.psea.api.exceptions.PseaCancellationException;
import com.requirementyogi.datacenter.psea.db.dao.PseaTaskDAO;
import com.requirementyogi.datacenter.psea.db.entities.DBPseaTask;
import com.requirementyogi.datacenter.psea.dto.PseaTaskStatus;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static com.requirementyogi.datacenter.psea.dto.PseaTaskStatus.*;

@Component
@ExportAsService(PseaService.class)
public class PseaServiceImpl implements PseaService, InitializingBean, DisposableBean {

    private final static Logger LOG = LoggerFactory.getLogger(PseaServiceImpl.class);
    /** In the local home. Atlassian recommends it for exports, imports, etc. */
    private static final String TEMPORARY_DIRECTORY = "temp";
    /** In Confluence's temporary directory, so that our files are easy to spot. See {@link #getTemporaryDirectory()} */
    private static final String YOGI_TEMP_DIR = "yogi";
    /** No directory separator, and not "." or ".." */
    private static final Pattern VALID_FILE_NAME = Pattern.compile("^(?!\\.{1,2}$)[0-9A-Za-z._-]+$");
    private static final int MAX_FILE_NAME_LENGTH = 255;
    public static final String SETTINGS_ROOT = "com.requirementyogi.psea";
    private static final String SETTINGS_ROW_LIMIT = SETTINGS_ROOT + ".row-limit";
    private static final String SETTINGS_TIME_LIMIT = SETTINGS_ROOT + ".time-limit";
    private static final String SETTINGS_DATA_LIMIT = SETTINGS_ROOT + ".data-limit";
    private static final String SETTINGS_CONCURRENT_JOBS_LIMIT = SETTINGS_ROOT + ".jobs-limit";
    public static final long MAX_ROWS_DEFAULT = 1000000; // Straight out of Excel 2007's limits
    public static final long TIME_LIMIT_DEFAULT = TimeUnit.MINUTES.toMillis(2); // The default in RY
    public static final long TIME_LIMIT_MAX = TimeUnit.HOURS.toMillis(2); // 2 hours shall be enough...
    public static final long DATA_LIMIT_DEFAULT = 100000000; // 100 MB of data ?
    public static final long DATA_LIMIT_MAX = 100 * DATA_LIMIT_DEFAULT; // 10GB of data...
    public static final long CONCURRENT_JOBS_DEFAULT = 10;
    /** Tasks running for longer than this factor x the time limit are considered abandoned, and marked as failed */
    private static final long STALE_TASK_FACTOR = 2;
    public static final long CONCURRENT_JOBS_MAX = 10000 * CONCURRENT_JOBS_DEFAULT; // Doesn't really matter, they can be small

    private final PluginSettingsFactory pluginSettingsFactory;
    private final ApplicationProperties applicationProperties;
    private final BootstrapManager bootstrapManager;
    private final AccessModeService accessModeService;
    private final PseaTaskDAO dao;
    private final ActiveObjects ao;
    private final String version;
    private ExecutorService executorService; // Will be assigned after the constructor

    /**
     * The monitoring task. Removing items from the ThreadLocal is guaranteed by the fact that it's managed in a
     * lambda.
     */
    private final ThreadLocal<Long> threadLocalTaskId = new ThreadLocal<>();

    public PseaServiceImpl(@ComponentImport PluginSettingsFactory pluginSettingsFactory,
                           @ComponentImport ApplicationProperties applicationProperties,
                           @ComponentImport BootstrapManager bootstrapManager,
                           @ComponentImport AccessModeService accessModeService,
                           PseaTaskDAO dao,
                           @ComponentImport ActiveObjects ao,
                           InternalBeanFactory internalBeanFactory
    ) {
        this.pluginSettingsFactory = pluginSettingsFactory;
        this.applicationProperties = applicationProperties;
        this.bootstrapManager = bootstrapManager;
        this.accessModeService = accessModeService;
        this.dao = dao;
        this.ao = ao;
        this.version = internalBeanFactory.getPluginVersion(PluginUtil.PLUGIN_PSEA_KEY);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.executorService = Executors.newCachedThreadPool();
    }

    @Override
    public String getVersion() {
        return version;
    }

    /**
     * Creates a record in the PSEA database to monitor (and be able to cancel) the task executed by 'callable'.
     * It is expected that 'callable' will call pseaService.export().
     *
     * We couldn't have done is inside of pseaService.export() because backward-compatibility is required.
     *
     * @param taskDetails a map of String -> anything, containing the details of the task.
     *                    For PSEA 1.8.0, the only recognized key is 'details'.
     * @param callable the job to execute when the export is ready/possible
     * @param transactionHasAlreadyStarted if true, that means AO (and probably the XWork action itself) has already
     *                                     started the transaction, making it impossible to save-and-flush the status
     *                                     of the task to the DB. If so, this method will start a thread, where the
     *                                     transaction is clean, and execute the 'callable' inside.
     * @throws PseaCancellationException
     */
    @Override
    public <T> T startMonitoredTask(
            Map<String, Object> taskDetails,
            Callable<T> callable,
            long waitingTimeMillis,
            boolean transactionHasAlreadyStarted
    ) throws PseaCancellationException {
        try {
            return ensureTransactionIsClean(transactionHasAlreadyStarted, () -> {
                try {
                    DBPseaTask record = createTask(PseaTaskStatus.PREPARING, waitingTimeMillis, taskDetails);
                    if (record != null) {
                        threadLocalTaskId.set(record.getID());
                    }
                    return callable.call();
                } finally {
                    Long taskId = threadLocalTaskId.get();
                    threadLocalTaskId.remove();
                    DBPseaTask task = dao.get(taskId);
                    if (task != null) {
                        PseaTaskStatus status = PseaTaskStatus.of(task.getStatus());
                        if (status != null && status.isRunning()) {
                            if (status == PseaTaskStatus.CANCELLING) {
                                dao.save(task, PseaTaskStatus.CANCELLED, "Shut down by cancellation");
                            } else {
                                dao.save(task, ERROR, "Status is " + status + " at the end of the task");
                            }
                        }
                    }
                }
            });
        } catch (Exception ex) {
            // Unwrap the exception
            while ((ex instanceof UndeclaredThrowableException || ex instanceof InvocationTargetException)
                && ex.getCause() instanceof Exception
                && ex != ex.getCause()) {
                ex = (Exception) ex.getCause();
            }
            // We don't write any message, so the default message is the exception's message, and it looks better for the UI.
            throw new RuntimeException(ex);
        }
    }

    private <T> T ensureTransactionIsClean(boolean transactionHasAlreadyStarted, Callable<T> callable) throws Exception {
        // We're executing in a separate thread, just because of transactions. Yes, XWork wraps Actions into
        // transactions, so if we don't execute in a separate thread, our .executeInTransaction() have no effect.
        // But we still have to carry the current user to that thread.
        if (transactionHasAlreadyStarted) {
            ConfluenceUser user = AuthenticatedUserThreadLocal.get();
            Callable<T> task = () -> {
                try {
                    AuthenticatedUserThreadLocal.set(user);
                    return callable.call();
                } finally {
                    AuthenticatedUserThreadLocal.reset();
                }
            };
            Future<T> future = executorService.submit(task);
            long timeoutMillis = getTimeLimit() + TimeUnit.SECONDS.toMillis(60);
            try {
                return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                future.cancel(true);
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interruption while executing a PSEA task", e);
            } catch (TimeoutException e) {
                future.cancel(true);
                throw new RuntimeException("PSEA task exceeded the time limit of " + timeoutMillis + "ms", e);
            } catch (ExecutionException e) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                } else {
                    throw new RuntimeException("Exception while waiting for a PSEA-monitored task to finish", e);
                }
            }
        } else {
            return callable.call();
        }
    }

    /**
     * Creates a task in the database, to monitor exports.
     *
     * This method will Thread.sleep() until it is available.
     *
     * @param waitingTimeMillis if the task needs to be created, and if the number of jobs is above the limit,
     *                          it will retry for maximum waitingTimeMillis before throwing a PseaCancellationException.
     *                          If 0L is passed, then it will either pass or reject straight away, but not wait.
     */
    DBPseaTask createTask(PseaTaskStatus status, long waitingTimeMillis, Map<String, Object> taskDetails) throws PseaCancellationException {
        if (accessModeService.getAccessMode() != AccessMode.READ_WRITE) {
            return null;
        }
        Long taskId = threadLocalTaskId.get();
        DBPseaTask record = null;
        if (taskId != null) {
            record = dao.get(taskId);
        }
        if (record == null) {
            record = dao.create(
                    NOT_STARTED,
                    AuthenticatedUserThreadLocal.get(),
                    taskDetails != null ? (String) taskDetails.get("details") : null
            );

            waitUntilASlotIsAvailable(waitingTimeMillis, record);

            dao.save(record, status, null);
        } else {
            // The record already exists (nested createTask call from export()): transition its status
            // if we are moving to a different running state, so the UI reflects actual progress
            // (otherwise the task appears stuck in PREPARING for the entire callback).
            PseaTaskStatus current = PseaTaskStatus.of(record);
            if (current != null && current.isRunning() && current != CANCELLING && current != status) {
                dao.save(record, status, null);
            }
        }
        return record;
    }

    private void waitUntilASlotIsAvailable(long waitingTimeMillis, DBPseaTask record) throws PseaCancellationException {
        long maxConcurrentJobs = getConcurrentJobsLimit();

        // Abandoned tasks (e.g. after a restart during an export) would otherwise take the slots forever
        int staleTasks = dao.markStaleTasksAsFailed(STALE_TASK_FACTOR * getTimeLimit());
        if (staleTasks > 0) {
            LOG.warn("PSEA: {} task(s) were still running after {} times the time limit, they were marked as failed.",
                     staleTasks, STALE_TASK_FACTOR);
        }

        long timeout = System.currentTimeMillis() + waitingTimeMillis;
        int runningJobs;
        // Retry 10 times during the duration, but at least every 3s and at most every 10s.
        long sleepTime = Math.max(3000L, Math.min((timeout - System.currentTimeMillis()) / 10L, 10000L));
        while ((runningJobs = dao.countRunningJobs()) >= maxConcurrentJobs) {
            if (System.currentTimeMillis() > timeout) {
                String message = "Cancelled because too many concurrent jobs are running (" + runningJobs + "/" + maxConcurrentJobs + ")";
                dao.save(record, CANCELLED, message);
                throw new PseaCancellationException(message);
            }
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore the interruption bit
                throw new PseaCancellationException("Interrupted while waiting to start");
            }
        }
    }

    @Override
    public File export(String fileName, Consumer<WorkbookAPI> pluginCallback) throws PseaCancellationException {
        // The length first, so that the regular expression only runs on short inputs
        if (fileName == null || fileName.length() > MAX_FILE_NAME_LENGTH || !VALID_FILE_NAME.matcher(fileName).matches()) {
            throw new IllegalArgumentException("Invalid file name for an export: " + fileName);
        }
        File directory = getTemporaryDirectory();
        try {
            // Unlike mkdirs(), it doesn't fail if another export creates the directory at the same time
            Files.createDirectories(directory.toPath());
        } catch (IOException exception) {
            throw new RuntimeException("Can't create the directory " + directory, exception);
        }
        ExcelExportTask task = new ExcelExportTask(this, dao);
        return task.export(new File(directory, fileName), pluginCallback);
    }

    /**
     * "yogi" in Confluence's temporary directory:
     * - The directory configured in confluence.cfg.xml ("struts.multipart.saveDir", formerly
     *   "webwork.multipart.saveDir"), if any,
     * - Otherwise "temp" in the local home, i.e. {@code <confluence-home>/temp/yogi},
     * - Otherwise java.io.tmpdir, if the local home is unknown.
     */
    @Override
    @Nonnull
    public File getTemporaryDirectory() {
        String configuredDirectory = StringUtils.trimToNull(bootstrapManager.getFilePathProperty(ConfluenceBootstrapConstants.TEMP_DIR_PROP));
        if (configuredDirectory != null) {
            if (new File(configuredDirectory).isAbsolute()) {
                return new File(configuredDirectory, YOGI_TEMP_DIR);
            }
            // It would be resolved against the working directory of the JVM, usually in the installation directory
            LOG.warn("PSEA: The temporary directory of confluence.cfg.xml isn't absolute, so it is ignored: {}", configuredDirectory);
        }
        File temporaryDirectory = applicationProperties.getLocalHomeDirectory()
                                                       .map(localHome -> localHome.resolve(TEMPORARY_DIRECTORY).toFile())
                                                       .orElseGet(() -> new File(System.getProperty("java.io.tmpdir")));
        return new File(temporaryDirectory, YOGI_TEMP_DIR);
    }

    public void extract(PseaInput workbookFile, ExcelImportConsumer consumer) throws OutOfMemoryError, PSEAImportException {
        String fileName = workbookFile.getFileName();
        ExtractionTask extraction;
        if (fileName != null && fileName.endsWith(".csv")) {
            extraction = new CSVExtractionTask(ao, consumer);
        } else {
            extraction = new ExcelExtractionTask(ao, consumer);
        }
        extraction.extract(workbookFile);
    }

    private static long readLong(Object value, long min, long max, long defaultValue) {
        if (value instanceof String) {
            try {
                long limit = Long.parseLong((String) value);
                if (limit < min) limit = min;
                if (limit > max) limit = max;
                return limit;
            } catch (NumberFormatException nfe) {
                return defaultValue;
            }
        } else {
            return defaultValue;
        }
    }

    /**
     * Reads a setting in a transaction. In Confluence, plugin settings are stored by Bandana, which requires a
     * Hibernate session when the value isn't in the cache. Background threads (e.g. Requirement Yogi's queue)
     * don't have one, so it would fail with "Failed to find org.hibernate.Session from the current thread".
     */
    private Object readSetting(String key) {
        return ao.executeInTransaction(() -> pluginSettingsFactory.createGlobalSettings().get(key));
    }

    public Long getRowLimit() {
        Object value = readSetting(SETTINGS_ROW_LIMIT);
        return readLong(value, 1, MAX_ROWS_DEFAULT, MAX_ROWS_DEFAULT);
    }

    public void setRowLimit(Long limit) {
        if (accessModeService.isReadOnlyAccessModeEnabled()) {
            LOG.warn("PSEA settings were not saved because the instance is in read-only mode");
            return; // Don't save, silently.
        }
        PluginSettings settings = pluginSettingsFactory.createGlobalSettings();
        if (limit == null) {
            settings.remove(SETTINGS_ROW_LIMIT);
        } else {
            settings.put(SETTINGS_ROW_LIMIT, String.valueOf(limit));
        }
    }

    public Long getTimeLimit() {
        Object value = readSetting(SETTINGS_TIME_LIMIT);
        return readLong(value, 1, TIME_LIMIT_MAX, TIME_LIMIT_DEFAULT);
    }

    public void setTimeLimit(Long limit) {
        if (accessModeService.isReadOnlyAccessModeEnabled()) {
            LOG.warn("PSEA settings were not saved because the instance is in read-only mode");
            return; // Don't save, silently.
        }
        PluginSettings settings = pluginSettingsFactory.createGlobalSettings();
        if (limit == null) {
            settings.remove(SETTINGS_TIME_LIMIT);
        } else {
            settings.put(SETTINGS_TIME_LIMIT, String.valueOf(limit));
        }
    }

    public long getDataLimit() {
        Object value = readSetting(SETTINGS_DATA_LIMIT);
        return readLong(value, 1, DATA_LIMIT_MAX, DATA_LIMIT_DEFAULT);
    }

    public void setConcurrentJobsLimit(Long limit) {
        if (accessModeService.isReadOnlyAccessModeEnabled()) {
            LOG.warn("PSEA settings were not saved because the instance is in read-only mode");
            return; // Don't save, silently.
        }
        PluginSettings settings = pluginSettingsFactory.createGlobalSettings();
        if (limit == null) {
            settings.remove(SETTINGS_CONCURRENT_JOBS_LIMIT);
        } else {
            settings.put(SETTINGS_CONCURRENT_JOBS_LIMIT, String.valueOf(limit));
        }
    }

    public long getConcurrentJobsLimit() {
        Object value = readSetting(SETTINGS_CONCURRENT_JOBS_LIMIT);
        return readLong(value, -1, CONCURRENT_JOBS_MAX, CONCURRENT_JOBS_DEFAULT);
    }

    public void setDataLimit(Long limit) {
        if (accessModeService.isReadOnlyAccessModeEnabled()) {
            LOG.warn("PSEA settings were not saved because the instance is in read-only mode");
            return; // Don't save, silently.
        }
        PluginSettings settings = pluginSettingsFactory.createGlobalSettings();
        if (limit == null) {
            settings.remove(SETTINGS_DATA_LIMIT);
        } else {
            settings.put(SETTINGS_DATA_LIMIT, String.valueOf(limit));
        }
    }

    @Override
    public void destroy() throws Exception {
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

}
