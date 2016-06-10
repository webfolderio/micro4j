package com.micro4j.persistence.exception;

import java.sql.SQLException;

public class TableCreateException extends PersistenceException {

    private static final long serialVersionUID = -4652887438758661696L;

    public TableCreateException(SQLException e) {
        super(e);
    }
}
