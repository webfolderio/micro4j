/* MIT License
 * 
 * Copyright (c) 2016 http://micro4j.com
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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

    public String generateCreateSql(DatabaseVendor vendor ) {
        String sql = null;
        if (! hasSize() && ! hasScale()) {
            sql = format(" %s %s", getName(), getJdbcType().toString());
        } else if (hasSize() && ! hasScale()) {
            sql = format(" %s %s(%s)", getName(), getJdbcType().toString(), getSize());
        } else {
            sql = format(" %s %s(%s,%s)", getName(), getJdbcType().toString(), getSize(), getScale());
        }
        if (isPrimaryKey()) {
            sql = sql + " PRIMARY KEY";
        }
        if ( ! isNullable()) {
            sql = sql + " NOT NULL";
        }
        return sql;
    }

    public String generateAlterDrop(String table, String schema, DatabaseVendor vendor) {
        switch (vendor) {
            default: return format("ALTER TABLE %s.%s DROP COLUMN %s", schema, table, getName());
        }
    }

    public String generateAlterAdd(String table, String schema, DatabaseVendor vendor) {
        String sql = generateCreateSql(vendor);
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
