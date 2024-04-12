package com.playsql.psea.impl;

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

import com.atlassian.activeobjects.external.ActiveObjects;
import com.google.common.collect.Lists;
import com.playsql.psea.api.*;
import com.playsql.psea.impl.beans.ImportableCellImpl;
import com.playsql.psea.impl.beans.ImportableRowImpl;
import com.playsql.psea.impl.beans.ImportableSheetImpl;
import com.playsql.psea.utils.Utils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Iterator;
import java.util.List;

/**
 * Extractor for CSV files, to make them look like XLS/XLSX files
 */
public class CSVExtractionTask extends ExtractionTask {

    private final static Logger LOG = LoggerFactory.getLogger(CSVExtractionTask.class);
    private final static String SHEET_NAME = "CSV";

    public CSVExtractionTask(ActiveObjects ao, ExcelImportConsumer rowConsumer) {
        super(ao, rowConsumer);
    }

    @Override
    public void extract(PseaInput workbookFile) throws OutOfMemoryError, PSEAImportException {
        clock = Utils.Clock.start("Reading CSV file " + workbookFile.getFileName() + " - ");
        int totalItems;
        try (Reader reader = getReader(workbookFile);
             CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT)
        ) {
            totalItems = (int) parser.stream().count();
        } catch (IOException e) {
            throw new PSEAImportException(workbookFile, e);
        }
        LOG.debug(clock.time("Done counting the rows"));

        int processedItems = 0;
        try (Reader reader = getReader(workbookFile);
             CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT)
        ) {
            if (rowConsumer.isSheetActive(SHEET_NAME)) {

                List<String> headerNames = parser.getHeaderNames();
                List<ImportableCell> headerCells = Lists.newArrayList();
                for (int i = 0 ; i < headerNames.size() ; i++) {
                    headerCells.add(new ImportableCellImpl(i, false, headerNames.get(i), null));
                }

                rowConsumer.consumeNewSheet(new ImportableSheetImpl(
                        SHEET_NAME,
                        new ImportableRowImpl(0, false, headerCells)
                ));

                Iterator<CSVRecord> iterator = parser.iterator();
                while (iterator.hasNext()) {
                    processedItems += ao.executeInTransaction(() -> {
                        rowConsumer.onNewTransaction();
                        int recordCount = 0;
                        while (recordCount++ < maxRecordsPerTransaction && iterator.hasNext()) {
                            CSVRecord record = iterator.next();
                            if (focusedRow != null && record.getRecordNumber() < focusedRow - 2 && record.getRecordNumber() > focusedRow + 2) {
                                continue;
                            }
                            List<ImportableCell> cells = Lists.newArrayList();
                            for (int i = 0; i < record.size(); i++) {
                                cells.add(new ImportableCellImpl(i, false, record.get(i), null));
                            }
                            rowConsumer.consumeImportableRow(new ImportableRowImpl(
                                    (int) record.getRecordNumber(),
                                    false,
                                    cells
                            ));
                            recordCount++;
                        }
                        return recordCount;
                    });
                    rowConsumer.setProgress(processedItems, totalItems);
                }
                rowConsumer.endOfSheet(SHEET_NAME);
            }
            rowConsumer.endOfWorkbook();
            rowConsumer.setProgress(totalItems, totalItems);
        } catch (IOException e) {
            throw new PSEAImportException(workbookFile, e);
        }
    }

    private Reader getReader(PseaInput pseaInput) throws IOException {
        if (pseaInput instanceof PseaFileInput) {
            return new FileReader(((PseaFileInput) pseaInput).getFile());
        } else if (pseaInput instanceof PseaInputStream) {
            return new InputStreamReader(((PseaInputStream) pseaInput).getInputStream());
        } else {
            throw new RuntimeException("Unknown PseaInput class: " + pseaInput);
        }
    }
}
