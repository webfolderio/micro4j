package com.micro4j.persistence.core;

import javax.sql.DataSource;

public class PersistenceConfiguration {

    private String defaultSchema;

    private String defaultSequence;

    private DataSource dataSource;

    public PersistenceConfiguration(String defaultSchema, String defaultSequence, DataSource dataSource) {
        this.defaultSchema = defaultSchema;
        this.defaultSequence = defaultSequence;
        this.dataSource = dataSource;
    }

    public String getDefaultSchema() {
        return defaultSchema;
    }

    public String getDefaultSequence() {
        return defaultSequence;
    }

    public DataSource getDataSource() {
        return dataSource;
    }
}
