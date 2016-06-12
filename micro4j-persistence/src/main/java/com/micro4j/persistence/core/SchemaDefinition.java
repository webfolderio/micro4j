package com.micro4j.persistence.core;

import java.util.ArrayList;
import java.util.List;

public class SchemaDefinition {

    private String name;

    private String sequence;

    private List<TableDefinition> definitions = new ArrayList<>();

    public SchemaDefinition(String name, String sequence, List<TableDefinition> definitions) {
        this.name = name;
        this.sequence = sequence;
        this.definitions = definitions;
    }

    public String getName() {
        return name;
    }

    public String getSequence() {
        return sequence;
    }

    public List<TableDefinition> getDefinitions() {
        return definitions;
    }

    @Override
    public String toString() {
        return "SchemaDefinition [name=" + name + ", sequence=" + sequence + ", definitions=" + definitions + "]";
    }
}
