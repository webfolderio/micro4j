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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
}
