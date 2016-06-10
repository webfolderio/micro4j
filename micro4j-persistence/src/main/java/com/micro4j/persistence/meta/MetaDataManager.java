package com.micro4j.persistence.meta;

import java.util.List;
import java.util.Optional;

import com.micro4j.persistence.core.DatabaseVendor;
import com.micro4j.persistence.core.TableDefinition;

public interface MetaDataManager {

    Optional<TableDefinition> getTable(String tableName);

    Optional<TableDefinition> getTable(String schema, String tableName);

    List<String> getTables(String schema);

    List<String> getTables();

    DatabaseVendor getVendor();

    boolean hasSchema(String schema);

    List<String> listSequences(String schema);
}
