package com.micro4j.persistence.alter;

import com.micro4j.persistence.meta.ColumnDefinition;

public class Alter {

    private ColumnDefinition column;

    private AlterType type;

    public Alter(ColumnDefinition column, AlterType type) {
        this.column = column;
        this.type = type;
    }

    public ColumnDefinition getColumn() {
        return column;
    }

    public AlterType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "Alter [column=" + column + ", type=" + type + "]";
    }
}
