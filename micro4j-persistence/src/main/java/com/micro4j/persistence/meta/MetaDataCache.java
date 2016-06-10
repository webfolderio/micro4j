package com.micro4j.persistence.meta;

import com.micro4j.persistence.core.TableDefinition;

interface MetaDataCache {

    TableDefinition getTable(String entityName, String schema);
}
