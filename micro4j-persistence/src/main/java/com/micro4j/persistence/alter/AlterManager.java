package com.micro4j.persistence.alter;

import java.util.List;

import com.micro4j.persistence.core.TableDefinition;

public interface AlterManager {

    void create(TableDefinition table);

    void drop(TableDefinition table);

    List<Alter> prepareAlter(TableDefinition existingTable, TableDefinition newTable);

    void executeAlter(TableDefinition table, List<Alter> alters);

    boolean addListener(AlterListener listener);

    boolean removeListener(AlterListener listener);

    boolean createSequence(String schema, String sequence);
}
