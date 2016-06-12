package com.micro4j.persistence.exception;

public class SchemaParseException extends PersistenceException {

    private static final long serialVersionUID = 1230924295934847575L;

    public SchemaParseException(String message) {
        super(message);
    }

    public SchemaParseException(Exception e) {
        super(e);
    }
}
