package com.playsql.psea.api;

/*
 * #%L
 * Play SQL Exports
 * %%
 * Copyright (C) 2016 - 2018 Play SQL S.A.S.U.
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

import com.google.common.collect.Lists;

import javax.xml.bind.annotation.XmlTransient;
import java.util.List;

/**
 * A class to store metadata from Excel Workbook
 */
public final class Workbook {

    /**
     * original excel file name
     */
    private String name;

    /**
     *
     */
    private String storedResourceId;

    /**
     *
     */
    private IntegrationState integrationState;

    public enum IntegrationState {

        PENDING("PENDING"),
        COMPLETED("COMPLETED");
        private final String value;

        IntegrationState(String value){
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    /**
     * worksheets in the workbook
     */
    private List<Worksheet> worksheets;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public IntegrationState getIntegrationState() {
        return integrationState;
    }

    public void setIntegrationState(IntegrationState integrationState) {
        this.integrationState = integrationState;
    }

    public List<Worksheet> getWorksheets() {
        return worksheets;
    }

    public void setWorksheets(List<Worksheet> worksheets) {
        this.worksheets = worksheets;
    }

    public String getStoredResourceId() {return storedResourceId;}

    public void setStoredResourceId(String storedResourceId) {this.storedResourceId = storedResourceId;}

    /**
     * A class to store metadata from Excel worksheet
     */
    public static class Worksheet {

        @XmlTransient
        private Workbook workbook;
        /**
         * sheet name
         */
        private String name;

        /**
         * Integration configuration to requirement yogi
         */
        private IntegrationConfig integrationConfig;

        /**
         * rows in the sheet
         */
        private List<Row> rows = Lists.newArrayList();

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public IntegrationConfig getIntegrationConfig() {
            return integrationConfig;
        }

        public void setIntegrationConfig(IntegrationConfig integrationConfig) {
            this.integrationConfig = integrationConfig;
        }

        public List<Row> getRows() {
            return rows;
        }

        public void setRows(List<Row> rows) {
            this.rows = rows;
        }

        public Workbook getWorkbook() {
            return workbook;
        }

        public void setWorkbook(Workbook workbook) {
            this.workbook = workbook;
        }
    }

    /**
     * A class to store Integration configuration of a sheet to requirement yogi
     */
    public static class IntegrationConfig {
        /**
         * store how an excel sheet columns are mapped to a requirement properties
         */
        private List<ColumnMapping> columnsMapping;

        /**
         * a kind of space in which all data (requirement info) extracted from a sheet are grouped
         */
        private String category;

        public List<ColumnMapping> getColumnsMapping() {
            return columnsMapping;
        }

        public void setColumnsMapping(List<ColumnMapping> columnsMapping) {
            this.columnsMapping = columnsMapping;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }
    }

    /**
     * A class to store metadata from a row in a worksheet
     */
    public static class Row {

        @XmlTransient
        private Worksheet worksheet;

        /**
         *
         */
        private List<Cell> cells;
        /**
         *
         */
        private Integer rowNum;

        public Integer getRowNum() {
            return rowNum;
        }

        public void setRowNum(Integer rowNum) {
            this.rowNum = rowNum;
        }

        public List<Cell> getCells() {
            return cells;
        }

        public void setCells(List<Cell> cells) {
            this.cells = cells;
        }

        public Worksheet getWorksheet() {
            return worksheet;
        }

        public void setSheet(Worksheet worksheet) {
            this.worksheet = worksheet;
        }
    }

    /**
     * A class to store metadata from a cell in a row
     */
    public static class Cell {
        /**
         *
         */
        public Integer index;

        /**
         *
         */
        public String value;
        /**
         * default constructor
         */
        public Cell(){}

        public Cell(Integer index, String value) {
            this.index = index;
            this.value = value;
        }

        public Integer getIndex() {
            return index;
        }

        public void setIndex(Integer index) {
            this.index = index;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    /**
     * A class to store mapping information of a column to a requirement property
     */
    public static class ColumnMapping {
        public static final String REQUIREMENT_FIELD_KEY = "KEY";
        public static final String REQUIREMENT_FIELD_HTMLEXCERPT = "DESCRIPTION";
        public static final String REQUIREMENT_PROPERTY = "PROPERTY";

        /**
         * the name of a requirement field or a  "standalone" property
         */
        public String mapping;

        /**
         *
         */
        public Integer index;

        /**
         * default constructor
         */
        public ColumnMapping(){
        }

        public ColumnMapping(Integer index, String mapping) {
            this.index = index;
            this.mapping = mapping;
        }

        public Integer getIndex() {
            return index;
        }

        public void setIndex(Integer index) {
            this.index = index;
        }

        public String getMapping() {
            return mapping;
        }

        public void setMapping(String mapping) {
            this.mapping = mapping;
        }
    }

    private Worksheet getCurrentWorksheet(){
       return worksheets.get(worksheets.size()-1);
    }

    /**
     * Convenience method to add rows to last sheet
     * @param row
     */
    public void addRow(Row row){
        getCurrentWorksheet().getRows().add(row);
    }
}
