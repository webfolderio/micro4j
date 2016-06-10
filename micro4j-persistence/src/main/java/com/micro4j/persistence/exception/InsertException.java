package com.micro4j.persistence.exception;

import java.sql.SQLException;

public class InsertException extends PersistenceException {

    private static final long serialVersionUID = 583065335042966360L;

    public InsertException(SQLException e) {
        super(e);
    }
}
