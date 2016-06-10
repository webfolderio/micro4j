package com.micro4j.persistence.meta;

import com.micro4j.persistence.core.TableDefinition;

public interface MetaDataCache {

    TableDefinition getTable(String entityName, String schema);
}
