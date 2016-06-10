package com.micro4j.persistence.core;

import static java.lang.String.format;

import java.sql.JDBCType;

public class ColumnDefinition {

    private String name;

    private JDBCType jdbcType;

    private int size;

    private int scale;

    private String sequence;

    private boolean primaryKey;

    private boolean nullable;

    public ColumnDefinition(String name) {
        this.name = name;
    }

    public ColumnDefinition(String name,
                                int size,
                                int scale,
                                JDBCType jdbcType,
                                boolean primaryKey,
                                boolean nullable,
                                String sequence) {
        this.name = name;
        this.size = size;
        this.scale = scale;
        this.jdbcType = jdbcType;
        this.primaryKey = primaryKey;
        this.nullable = nullable;
        this.sequence = sequence;
    }

    public String getName() {
        return name;
    }

    public ColumnDefinition setName(String name) {
        this.name = name;
        return this;
    }

    public JDBCType getJdbcType() {
        return jdbcType;
    }

    public ColumnDefinition setJdbcType(JDBCType jdbcType) {
        this.jdbcType = jdbcType;
        return this;
    }

    public ColumnDefinition setNullable(boolean nullable) {
        this.nullable = nullable;
        return this;
    }

    public int getSize() {
        return size;
    }

    public ColumnDefinition setSize(int size) {
        this.size = size;
        return this;
    }

    public int getScale() {
        return scale;
    }

    public ColumnDefinition setScale(int scale) {
        this.scale = scale;
        return this;
    }

    public ColumnDefinition setSequence(String sequence) {
        this.sequence = sequence;
        return this;
    }

    public ColumnDefinition setPrimaryKey(boolean primaryKey) {
        this.primaryKey = primaryKey;
        return this;
    }

    public boolean hasSize() {
        return size > 0;
    }

    public boolean hasScale() {
        return scale > 0;
    }

    public String getSequence() {
        return sequence;
    }

    public boolean isPrimaryKey() {
        return primaryKey;
    }

    public boolean isNullable() {
        return nullable;
    }

    public String getCreateSql(DatabaseVendor vendor ) {
        String sql = null;
        if (! hasSize() && ! hasScale()) {
            sql = format("%s %s", getName(), getJdbcType().toString());
        } else if (hasSize() && ! hasScale()) {
            sql = format("%s %s(%s)", getName(), getJdbcType().toString(), getSize());
        } else {
            sql = format("%s %s(%s,%s)", getName(), getJdbcType().toString(), getSize(), getScale());
        }
        if (isPrimaryKey()) {
            sql = sql + " PRIMARY KEY";
        }
        if ( ! isNullable()) {
            sql = sql + " NOT NULL";
        }
        return sql;
    }

    public String getAlterDrop(String table, String schema, DatabaseVendor vendor) {
        switch (vendor) {
            default: return format("ALTER TABLE %s.%s DROP COLUMN %s", schema, table, getName());
        }
    }

    public String getAlterAdd(String table, String schema, DatabaseVendor vendor) {
        String sql = getCreateSql(vendor);
        switch (vendor) {
            default:
            case hsql: return format("ALTER TABLE %s.%s ADD %s", schema, table, sql);
        }
    }

    @Override
    public String toString() {
        return "ColumnDefinition [name=" + name + ", jdbcType=" + jdbcType + ", size=" + size + ", scale=" + scale
                + ", sequence=" + sequence + ", primaryKey=" + primaryKey + ", nullable=" + nullable + "]";
    }
}
