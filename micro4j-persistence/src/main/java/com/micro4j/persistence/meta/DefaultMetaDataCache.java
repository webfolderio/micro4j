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

    private AlterManager alterManager;

    private ConcurrentMap<String, TableDefinition> tables = new ConcurrentHashMap<>();

    public DefaultMetaDataCache(AlterManager alterManager, MetaDataManager metaDataManager) {
        this.alterManager = alterManager;
        this.metaDataManager = metaDataManager;
        this.alterManager.addListener(this);
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
