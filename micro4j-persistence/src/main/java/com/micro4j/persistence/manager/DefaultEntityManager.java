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
package com.micro4j.persistence.manager;

import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.nCopies;

import java.sql.Connection;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import javax.sql.DataSource;

import com.micro4j.persistence.core.AlterManager;
import com.micro4j.persistence.core.ColumnDefinition;
import com.micro4j.persistence.core.DatabaseVendor;
import com.micro4j.persistence.core.PersistenceConfiguration;
import com.micro4j.persistence.core.TableDefinition;
import com.micro4j.persistence.exception.DeleteException;
import com.micro4j.persistence.exception.InsertException;
import com.micro4j.persistence.exception.PersistenceException;
import com.micro4j.persistence.exception.QueryException;
import com.micro4j.persistence.exception.UpdateException;

public class DefaultEntityManager implements EntityManager, Constants {

    private PersistenceConfiguration configuration;

    private MetaDataManager metaDataManager;

    private AlterManager alterManager;

    public DefaultEntityManager(PersistenceConfiguration configuration) {
        this.configuration = configuration;
        this.metaDataManager = new DefaultMetaDataManager(configuration);
        this.alterManager = new DefaultAlterManager(configuration, metaDataManager);
        this.alterManager.addListener(metaDataManager);
    }

    public DefaultEntityManager(PersistenceConfiguration configuration, MetaDataManager metaDataManager, AlterManager alterManager) {
        this.configuration = configuration;
        this.metaDataManager = metaDataManager;
        this.alterManager = alterManager;
    }

    @Override
    public long insert(String tableName, Map<String, Object> entity) {
        TableDefinition table = getTable(tableName);

        String schema = getSchema(tableName);

        Map<String, Object> map = filterEntity(table, entity);

        Date now = new Date();

        if (table.hasColumn(CREATE_DATE)) {
            map.put(CREATE_DATE, now);
        }
        if (table.hasColumn(UPDATE_DATE)) {
            map.put(UPDATE_DATE, now);
        }
        if (table.hasColumn(CREATED_BY)) {
            map.put(CREATED_BY, getUsername());
        }
        if (table.hasColumn(UPDATED_BY)) {
            map.put(UPDATED_BY, getUsername());
        }
        if (table.hasColumn(ACTIVE)) {
            map.put(ACTIVE, ACTIVE_STATUS);
        }

        long id = -1;

        if (table.hasColumn(ID)) {
            id = nextId(tableName);
            map.put(ID, id);
        }

        Set<String> columns = map.keySet();

        String query = new StringBuilder()
                                .append("INSERT INTO")
                                .append(" ")
                                .append(schema)
                                .append(".")
                                .append(tableName)
                                .append(" ( ")
                                .append(join(", ", columns))
                                .append(" ) ")
                                .append("VALUES ( ")
                                .append(join(", ", nCopies(columns.size(), "?")))
                                .append(" ) ")
                                .toString();

        try (Connection conn =
                        getDataSource(tableName).getConnection();
                            PreparedStatement pstmt = conn.prepareStatement(query)) {
            int i = 1;
            for (Entry<String, Object> entry : map.entrySet()) {
                ColumnDefinition column = table.getColumn(entry.getKey());
                JDBCType type = column.getJdbcType();
                if (JDBCType.TIMESTAMP.equals(type)) {
                    if (entry.getValue() instanceof Date) {
                        pstmt.setObject(i, new Timestamp(((Date) entry.getValue()).getTime()), type.getVendorTypeNumber());
                    } else {
                        pstmt.setObject(i, new Timestamp(Long.valueOf(entry.getValue().toString())), type.getVendorTypeNumber());
                    }
                } else {
                    pstmt.setObject(i, entry.getValue(), type.getVendorTypeNumber());
                }
                i += 1;
            }
            pstmt.executeUpdate();
            return id;
        } catch (SQLException e) {
            throw new InsertException(e);
        }
    }

    protected TableDefinition getTable(String tableName) {
        Optional<TableDefinition> found = metaDataManager.getTable(tableName);
        return found.get();
    }

    @Override
    public long insertSelective(String tableName, Map<String, Object> entity) {
        removeNullValues(entity);
        return insert(tableName, entity);
    }

    @Override
    public Map<String, Object> get(String tableName, long id) {
        TableDefinition table = getTable(tableName);
        String schema = getSchema(tableName);
        String query = format("SELECT * FROM %s.%s WHERE ID = ?", schema, tableName);
        try (Connection conn =
                getDataSource(tableName).getConnection();
                    PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int count = meta.getColumnCount();
                    Map<String, Object> map = new LinkedHashMap<>();
                    for (int i = 0; i < count; i++) {
                        String column = meta.getColumnName(i + 1);
                        if (table.hasColumn(column) ||
                                        DEFAULT_COLUMNS.contains(column)) {
                            Object value = rs.getObject(column);
                            map.put(column, value);
                        }
                    }
                    return map;
                } else {
                    return emptyMap();
                }
            }
        } catch (SQLException e) {
            throw new QueryException(e);
        }
    }

    @Override
    public List<Map<String, Object>> listAll(String tableName) {
        TableDefinition table = getTable(tableName);
        String schema = getSchema(tableName);
        String query = format("SELECT * FROM %s.%s WHERE ACTIVE = %s", schema, tableName, ACTIVE_STATUS);
        try (Connection conn =
                getDataSource(tableName).getConnection();
                    PreparedStatement pstmt = conn.prepareStatement(query)) {
            List<Map<String, Object>> rows = new ArrayList<>();
            try (ResultSet rs = pstmt.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int count = meta.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 0; i < count; i++) {
                        String column = meta.getColumnName(i + 1);
                        if (table.hasColumn(column) ||
                                        DEFAULT_COLUMNS.contains(column)) {
                            Object value = rs.getObject(column);
                            row.put(column, value);
                        }
                    }
                    rows.add(row);
                }
                return rows.size() == 0 ? emptyList() : rows;
            }
        } catch (SQLException e) {
            throw new QueryException(e);
        }
    }

    @Override
    public boolean update(String tableName, Map<String, Object> entity) {
        TableDefinition table = getTable(tableName);
        String schema = getSchema(tableName);

        Map<String, Object> map = filterEntity(table, entity);

        long id = (long) map.get(ID);

        DEFAULT_COLUMNS.forEach(column -> map.remove(column));

        map.put(UPDATE_DATE, new Date());
        map.put(UPDATED_BY, getUsername());

        StringBuilder builder = new StringBuilder()
                                        .append("UPDATE")
                                        .append(" ")
                                        .append(schema)
                                        .append(".")
                                        .append(tableName)
                                        .append(" set ");

        int i = 0;
        for (Entry<String, Object> entry : map.entrySet()) {
            builder.append(entry.getKey()).append(" = ?");
            if (i + 1 < map.size()) {
                builder.append(", ");
            }
            i += 1;
        }

        map.put(ID, id);

        builder.append(" where ID = ?");

        String query = builder.toString();

        try (Connection conn =
                getDataSource(tableName).getConnection();
                    PreparedStatement pstmt = conn.prepareStatement(query)) {
            int j = 1;
            for (Entry<String, Object> entry : map.entrySet()) {
                pstmt.setObject(j, entry.getValue());
                j += 1;
            }
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new UpdateException(e);
        }
    }

    @Override
    public boolean updateSelective(String tableName, Map<String, Object> entity) {
        removeNullValues(entity);
        return update(tableName, entity);
    }

    @Override
    public boolean delete(String tableName, long id) {
        String query = getDeleteQuery(tableName);
        try (Connection conn =
                        getDataSource(tableName).getConnection();
                            PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, getUsername());
            pstmt.setTimestamp(2, new Timestamp(new Date().getTime()));
            pstmt.setLong(3, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DeleteException(e);
        }
    }

    protected long nextId(String tableName) {
        DatabaseVendor vendor = getDatabaseVendor(tableName);
        String query = null;

        String schema = getSchema(tableName);
        String seqence = getSequence(tableName, "ID");

        switch (vendor) {
            case hsql:
                query = format("call NEXT VALUE FOR %s.%s", schema, seqence);
            break;
            case h2:
                query = format("select %s.%s.nextval from dual", schema, seqence);
            break;
            case oracle:
                query = format("select %s.%s.nextval FROM DUAL", schema, seqence);
            break;
            default:
                throw new PersistenceException("Unsupported vendor [" + vendor.name() + "]");
        }

        try (Connection conn = getDataSource(tableName).getConnection();
                                PreparedStatement pstmt = conn.prepareStatement(query);
                ResultSet rs = pstmt.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        } catch (SQLException e) {
            throw new QueryException(e);
        }
    }

    protected Map<String, Object> filterEntity(TableDefinition table, Map<String, Object> entity) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (Entry<String, Object> entry : entity.entrySet()) {
            String column = entry.getKey();
            if ( ! table.hasColumn(column)) {
                entity.remove(column);
            }
            map.put(column, entry.getValue());
        }
        return map;
    }

    protected void removeNullValues(Map<String, Object> entity) {
        new HashSet<>(entity.keySet())
            .stream()
            .filter(property -> entity.get(property) == null)
            .forEach(property -> entity.remove(property));
    }

    protected String getDeleteQuery(String tableName) {
        String schema = getSchema(tableName);
        String query = format("UPDATE %s.%s set ACTIVE = %s, UPDATED_BY = ?, UPDATE_DATE = ? where %s = ?",
                                        schema, tableName, PASSIVE_STATUS, ID);
        return query;
    }

    protected DataSource getDataSource(String tableName) {
        return configuration.getDataSource();
    }

    protected DatabaseVendor getDatabaseVendor(String tableName) {
        return metaDataManager.getVendor();
    }

    protected String getSchema(String tableName) {
        TableDefinition table = getTable(tableName);
        String schema = table.getSchema();
        if (schema == null) {
            return configuration.getSchema();
        }
        return schema;
    }

    protected String getSequence(String tableName, String columnName) {
        TableDefinition table = getTable(tableName);
        ColumnDefinition column = table.getColumn(columnName);
        String sequence = column.getSequence();
        if (sequence == null) {
            return configuration.getSequence();
        }
        return sequence;
    }

    protected String getUsername() {
        return DEFAULT_USER;
    }

    @Override
    public MetaDataManager getMetaDataManager() {
        return metaDataManager;
    }

    @Override
    public AlterManager getAlterManager() {
        return alterManager;
    }
}
