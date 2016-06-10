package com.micro4j.persistence.core;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.micro4j.persistence.meta.ColumnDefinition;

public class TableDefinition {

    private String name;

    private String schema;

    private Map<String, ColumnDefinition> columns = new LinkedHashMap<>();

    public TableDefinition(TableDefinition table) {
        name = table.name;
        schema = table.schema;
        for (String column : table.columns.keySet()) {
            ColumnDefinition existing = table.columns.get(column);
            columns.put(column,
                    new ColumnDefinition(existing.getName(),
                                existing.getSize(),
                                existing.getScale(),
                                existing.getJdbcType(),
                                existing.isPrimaryKey(),
                                existing.isNullable(),
                                existing.getSequence()));
        }
    }

    public TableDefinition(String schema,
                    String name) {
        this.schema = schema;
        this.name = name;
    }

    public TableDefinition(String name) {
        this(null, name);
    }
 
    public void add(ColumnDefinition column) {
        if ( ! columns.containsKey(column.getName())) {
            columns.put(column.getName(), column);
        }
    }

    public boolean hasColumn(String name) {
        return columns.containsKey(name);
    }

    public Set<String> columnNames() {
        return new HashSet<>(columns.keySet());
    }

    public List<ColumnDefinition> columns() {
        return new ArrayList<>(columns.values());
    }

    public void removeColumn(String columnName) {
        columns.remove(columnName);
    }

    public String getName() {
        return name;
    }

    public String getSchema() {
        return schema;
    }

    public ColumnDefinition getColumn(String name) {
        return columns.get(name);
    }

    public String generateCreateScript() {
        return format("CREATE TABLE %s.%s", schema, name);
    }

    public String generateDropScript() {
        return format("DROP TABLE %s.%s", schema, name);
    }

    @Override
    public String toString() {
        return "TableDefinition [name=" + name + ", schema=" + schema + ", columns=" + columns + "]";
    }
}
