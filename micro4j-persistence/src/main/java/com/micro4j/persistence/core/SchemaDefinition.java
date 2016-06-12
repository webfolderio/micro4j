package com.micro4j.persistence.core;

import java.util.ArrayList;
import java.util.List;

public class SchemaDefinition {

    private String schema;

    private String sequence;

    private List<TableDefinition> definitions = new ArrayList<>();

    public SchemaDefinition(String schema, String sequence, List<TableDefinition> definitions) {
        this.schema = schema;
        this.sequence = sequence;
        this.definitions = definitions;
    }

    public String getSchema() {
        return schema;
    }

    public String getSequence() {
        return sequence;
    }

    public List<TableDefinition> getDefinitions() {
        return definitions;
    }

    @Override
    public String toString() {
        return "SchemaDefinition [schema=" + schema + ", sequence=" + sequence + ", definitions=" + definitions + "]";
    }
}
