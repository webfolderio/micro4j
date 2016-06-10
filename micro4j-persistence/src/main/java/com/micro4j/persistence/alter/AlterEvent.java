package com.micro4j.persistence.alter;

public class AlterEvent {

    private String schema;

    private String tableName;

    public AlterEvent(String schema, String tableName) {
        this.schema = schema;
        this.tableName = tableName;
    }

    public String getSchema() {
        return schema;
    }

    public String getTableName() {
        return tableName;
    }

    @Override
    public String toString() {
        return "AlterEvent [schema=" + schema + ", tableName=" + tableName + "]";
    }
}
