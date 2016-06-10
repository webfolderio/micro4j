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

import static com.micro4j.persistence.core.Utils.camelCaseToTableName;
import static com.micro4j.persistence.core.Utils.tableNameToCamelCase;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.micro4j.persistence.alter.AlterEvent;
import com.micro4j.persistence.alter.AlterListener;
import com.micro4j.persistence.alter.AlterManager;
import com.micro4j.persistence.core.TableDefinition;

class DefaultMetaDataCache implements MetaDataCache, AlterListener {

    private MetaDataManager metaDataManager;

    private ConcurrentMap<String, TableDefinition> tables = new ConcurrentHashMap<>();

    public DefaultMetaDataCache(AlterManager alterManager, MetaDataManager metaDataManager) {
        this.metaDataManager = metaDataManager;
        alterManager.addListener(this);
    }

    @Override
    public TableDefinition getTable(String entityName, String schema) {
        TableDefinition table = tables.get(entityName);
        if (table == null) {
            String tableName = camelCaseToTableName(entityName);
            table = metaDataManager.getTable(schema, tableName).get();
            if (table != null) {
                TableDefinition existingTable = tables.putIfAbsent(entityName, table);
                if (existingTable != null) {
                    table = existingTable;
                }
            }
        }
        return table;
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
            .forEach(table -> tables.remove(tableNameToCamelCase(table.getName())));
    }

    public void dispose() {
        tables.clear();
    }
}
