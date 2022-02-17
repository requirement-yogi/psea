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

import javax.annotation.Nullable;
import java.io.File;
import java.io.InputStream;
import java.util.function.Consumer;

public interface PseaService {

    /**
     * Export excel file with default constraints
     *
     * @param f the consumer
     * @return File where the file is stored
     */
    File export(Consumer<WorkbookAPI> f);

    /**
     * Offers the possibility to delete a file before the JVM stops.
     * The implementation ensures the validity of the file before deleting it.
     *
     * @return true if the file is successfully deleted
     * @since 1.7
     */
    boolean deleteFile(File file);

    /**
     * Returns the limit of rows configured by the system administrator.
     *
     * The default is 1m, but null can be returned.
     *
     * @since 1.7
     */
    Long getRowLimit();

    /**
     * Sets the row limit of all exports.
     * @since 1.7
     */
    void setRowLimit(Long limit);

    /**
     * Get the time limit in milliseconds of all exports.
     *
     * The default is 1m, but null can be returned.
     *
     * @since 1.7
     */
    Long getTimeLimit();

    /**
     * Sets the time limit in milliseconds for all exports
     * @since 1.7
     */
    void setTimeLimit(Long timeLimit);

    /**
     * Read an Excel file, and feed it to the consumer
     * @param file the file, either a physical file or an InputStream
     * @param consumer the consumer
     * @throws OutOfMemoryError if the underlying Apache POI throws an OOME. It is recommended to catch this exception in the calling method.
     * @throws PSEAImportException if the file is not in a readable format, or if an IOException was encountered
     */
    void extract(PseaInput file, ExcelImportConsumer consumer) throws OutOfMemoryError, PSEAImportException;

    /**
     * A file or an inputstream that is used as an input for the Excel import
      */
    interface PseaInput {
        String getFileName();
    }

    class PseaFileInput implements PseaInput {
        private final File file;
        private final String fileName;

        public PseaFileInput(File file, String fileName) {
            this.file = file;
            this.fileName = fileName;
        }

        public File getFile() {
            return file;
        }

        @Override
        public String getFileName() {
            return file.getName();
        }
    }
    class PseaInputStream implements PseaInput {
        private final InputStream inputStream;
        private final String fileName;

        public PseaInputStream(InputStream inputStream, String fileName) {
            this.inputStream = inputStream;
            this.fileName = fileName;
        }

        public InputStream getInputStream() {
            return inputStream;
        }

        @Override
        public String getFileName() {
            return fileName;
        }
    }

}
