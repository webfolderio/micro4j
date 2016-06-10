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
package com.micro4j.persistence.meta;

import static com.micro4j.persistence.core.DatabaseVendor.h2;
import static com.micro4j.persistence.core.DatabaseVendor.hsql;
import static com.micro4j.persistence.core.DatabaseVendor.oracle;
import static java.lang.String.format;
import static java.util.Collections.unmodifiableList;
import static java.util.Locale.ENGLISH;
import static java.util.stream.Collectors.toList;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.JDBCType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.sql.DataSource;

import com.micro4j.persistence.alter.AlterEvent;
import com.micro4j.persistence.core.ColumnDefinition;
import com.micro4j.persistence.core.DatabaseVendor;
import com.micro4j.persistence.core.PersistenceConfiguration;
import com.micro4j.persistence.core.TableDefinition;
import com.micro4j.persistence.exception.PersistenceException;

public class DefaultMetaDataManager implements MetaDataManager {

    private PersistenceConfiguration configuration;

    private static final String COLUMN_NAME = "COLUMN_NAME";

    private static final String TABLE_NAME = "TABLE_NAME";

    private static final String TYPE_NAME = "TYPE_NAME";

    private static final String COLUMN_SIZE = "COLUMN_SIZE";

    private static final String DECIMAL_DIGITS = "DECIMAL_DIGITS";

    private static final String IS_NULLABLE = "IS_NULLABLE";

    private static final String TABLE_SCHEM = "TABLE_SCHEM";

    private static final String YES = "YES";

    private Map<String, String> typeAlias = new HashMap<>();

    private ConcurrentMap<String, TableDefinition> tables = new ConcurrentHashMap<>();

    private DatabaseVendor vendor;

    public DefaultMetaDataManager(PersistenceConfiguration configuration) {
        this.configuration = configuration;
        typeAlias.put("CHARACTER", "CHAR");
        typeAlias.put("VARCHAR2", "VARCHAR");
        typeAlias.put("NUMBER", "NUMERIC");
    }

    @Override
    public Optional<TableDefinition> getTable(String tableName) {
        return getTable(configuration.getSchema(), tableName);
    }

    public Optional<TableDefinition> getTable(String schema, String tableName) {
        TableDefinition table = null;

        String qualifiedName = format("%s.%s", schema, tableName);
        table = tables.get(qualifiedName);
        if (table != null) {
            return Optional.of(table);
        }

        DataSource ds = configuration.getDataSource();
        try (Connection conn = ds.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            Set<String> primaryKeys = new HashSet<>();
            try (ResultSet rsPrimaryKey = meta.getPrimaryKeys(null, schema, tableName)) {
                while (rsPrimaryKey.next()) {
                    String primaryKey = rsPrimaryKey.getString(COLUMN_NAME);
                    primaryKeys.add(primaryKey);
                }
            }
            try (ResultSet rs = meta.getColumns(null, schema, tableName, null)) {
                while (rs.next()) {
                    if (table == null) {
                        table = new TableDefinition(schema, tableName);
                    }
                    String name = rs.getString(COLUMN_NAME);
                    int size = rs.getInt(COLUMN_SIZE);
                    int scale = rs.getInt(DECIMAL_DIGITS);
                    String typeName = rs.getString(TYPE_NAME);
                    boolean primaryKey = primaryKeys.contains(name);
                    boolean nullable = YES.equalsIgnoreCase(rs.getString(IS_NULLABLE));
                    int start = typeName.indexOf('(');
                    if (start > 0) { // Remove the size & scale, TIMESTAMP(6) => TIMESTAMP
                        typeName = typeName.substring(0, start);
                    }
                    if (typeAlias.containsKey(typeName)) {
                        typeName = typeAlias.get(typeName);
                    }
                    JDBCType jdbcType = JDBCType.valueOf(typeName);
                    ColumnDefinition column = new ColumnDefinition(name, size, scale, jdbcType, primaryKey, nullable, null);
                    table.add(column);
                }
            }
            if (table != null) {
                TableDefinition existingTable = tables.putIfAbsent(qualifiedName, table);
                if (existingTable != null) {
                    table = existingTable;
                }
                return Optional.of(table);
            } else {
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public List<String> getTables(String schema) {
        DataSource ds = configuration.getDataSource();
        List<String> tables = new ArrayList<>();
        try (Connection conn = ds.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getTables(null, schema, null, null)) {
                while (rs.next()) {
                    String name = rs.getString(TABLE_NAME);
                    tables.add(name);
                }
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
        return tables;
    }

    @Override
    public List<String> getTables() {
        return getTables(configuration.getSchema());
    }

    @Override
    public boolean hasSchema(String schema) {
        DataSource ds = configuration.getDataSource();
        try (Connection conn = ds.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getSchemas(null, schema)) {
                if (rs.next()) {
                    return schema.equals(rs.getString(TABLE_SCHEM));
                } else {
                    return false;
                }
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public DatabaseVendor getVendor() {
        if (vendor == null) {
            DataSource ds = configuration.getDataSource();
            try (Connection conn = ds.getConnection()) {
                DatabaseMetaData meta = conn.getMetaData();
                String driverName = meta.getDriverName().toLowerCase(ENGLISH);
                vendor = driverName.contains(oracle.toString()) ?
                                        oracle :
                                        driverName.contains(hsql.toString()) ? hsql :
                                            driverName.contains(h2.toString()) ? h2 : oracle;
            } catch (SQLException e) {
                throw new PersistenceException(e);
            }
        }
        return vendor;
    }

    @Override
    public List<String> listSequences(String schema) {
        DatabaseVendor vendor = getVendor();
        DataSource ds = configuration.getDataSource();
        List<String> sequences = new ArrayList<>();

        String query = null;
        if (hsql.equals(vendor) || h2.equals(vendor)) {
            query = "SELECT * FROM information_schema.sequences";
        }

        if (query != null) {
            try (Connection conn = ds.getConnection();
                        Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery(query)) {
                    if (rs.next()) {
                        String name = rs.getString("SEQUENCE_SCHEMA");
                        if (name.equals(schema)) {
                            String sequence = rs.getString("SEQUENCE_NAME");
                            sequences.add(sequence);
                        }
                    }
                }
            } catch (SQLException e) {
                throw new PersistenceException(e);
            }
        } else {
            throw new PersistenceException("Method not implemented for the "
                                            + vendor.toString() + " jdbc driver");
        }
        return unmodifiableList(sequences);
    }

    @Override
    public void onTableDrop(AlterEvent event) {
        removeTable(event);
    }

    @Override
    public void onTableCreate(AlterEvent event) {
        removeTable(event);
    }

    @Override
    public void onTableAlterColumn(AlterEvent event) {
        removeTable(event);
    }

    protected void removeTable(AlterEvent event) {
        List<TableDefinition> removeList = tables
                                            .values()
                                            .stream()
                                            .filter(table -> event.getTableName().equals(table.getName()))
                                            .filter(table -> event.getSchema().equals(table.getSchema()))
                                            .collect(toList());
        removeList
            .forEach(table -> tables.remove(format("%s.%s", table.getSchema(), table.getName())));
    }
}
