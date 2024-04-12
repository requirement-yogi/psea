package com.playsql.psea.impl;

import com.playsql.psea.api.WorkbookAPI;
import com.playsql.psea.api.exceptions.PseaCancellationException;
import com.playsql.psea.db.dao.PseaTaskDAO;
import com.playsql.psea.db.entities.DBPseaTask;
import com.playsql.psea.dto.DTOPseaTask;
import com.playsql.psea.dto.PseaLimitException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.function.Consumer;

import static com.playsql.psea.dto.DTOPseaTask.Status.*;
import static com.playsql.psea.dto.DTOPseaTask.Status.ERROR;

/**
 * Implementation of the export into an Excel file
 */
public class ExcelExportTask {

    private final static Logger LOG = LoggerFactory.getLogger(PseaServiceImpl.class);

    private final PseaServiceImpl pseaService;
    private final PseaTaskDAO dao;

    public ExcelExportTask(PseaServiceImpl pseaService, PseaTaskDAO dao) {

        this.pseaService = pseaService;
        this.dao = dao;
    }

    public File export(Consumer<WorkbookAPI> pluginCallback) throws PseaCancellationException {
        // We don't wait, in this step, because callers who want to wait should do it in the startMonitoredTask,
        // assuming they have the correct version of PSEA.
        DBPseaTask record = pseaService.createTask(DTOPseaTask.Status.IN_PROGRESS, 0L, null);

        long sizeLimit = pseaService.getDataLimit();
        Consumer<Long> saveSize = buildSaveSizeFunction(record, sizeLimit);

        XSSFWorkbook xlWorkbook = null;
        long rowLimit = pseaService.getRowLimit();
        long timeLimit = pseaService.getTimeLimit();
        File file = null;
        try {
            xlWorkbook = new XSSFWorkbook();
            WorkbookAPIImpl workbook = new WorkbookAPIImpl(xlWorkbook, rowLimit, timeLimit, saveSize);
            try {
                pluginCallback.accept(workbook);
                dao.save(record, WRITING, null);
            } catch (PseaLimitException re) {
                dao.save(record, ERROR, re.getMessage());
                workbook.writeErrorMessageOnFirstSheet(re.getMessage());
                // We don't rethrow the error, because we want to save and return this spreadsheet to the user,
                // with the error inside.
            }

            file = File.createTempFile(pseaService.FILE_PREFIX, pseaService.FILE_EXTENSION);
            file.deleteOnExit();
            if (record != null) {
                record.setFilename(file.getName());
                dao.saveAndCheckCancellation(record);
            }

            try (FileOutputStream fileOut = new FileOutputStream(file)) {
                xlWorkbook.write(fileOut);
            }

            dao.save(record, DONE, "Size=" + workbook.getDataSize() + " units");
            return file;

        } catch (IOException e) {
            dao.save(record, ERROR, "IO Exception: " + e.getMessage());
            if (file != null) {
                file.delete(); // Doesn't throw an exception, just returns false if error
            }
            throw new RuntimeException("Error while writing an Excel file to disk", e);

        } catch (RuntimeException re) {
            dao.save(record, ERROR, re.getMessage());
            if (file != null) {
                file.delete(); // Doesn't throw an exception, just returns false if error
            }
            throw re;
        } finally {
            if (record != null) {
                DTOPseaTask.Status status = DTOPseaTask.Status.of(record);
                if (status == null || !status.isFinalState()) {
                    dao.save(record, ERROR, "The status was " + record.getStatus() + " despite the export being finished.");
                }
            }
            if (xlWorkbook != null) {
                try {
                    xlWorkbook.close();
                } catch (IOException e) {
                    LOG.error("Error while closing an Excel file that we were creating", e);
                }
            }
        }
    }

    private Consumer<Long> buildSaveSizeFunction(DBPseaTask record, long sizeLimit) {
        Consumer<Long> saveSize = new Consumer<Long>() {

            private final long SIZE_RESOLUTION = 10000; // Save the size every 10KB.
            private long nextSaveSize = 10; // We save just after a few bytes

            @Override
            public void accept(Long currentSize) {
                if (currentSize > sizeLimit) {
                    throw new PseaLimitException(currentSize, sizeLimit, "size", "units");
                }

                if (record != null && currentSize > nextSaveSize) {
                    nextSaveSize = currentSize + SIZE_RESOLUTION;
                    record.setSize(currentSize);
                    dao.saveAndCheckCancellation(record);
                }
            }
        };
        return saveSize;
    }
}
