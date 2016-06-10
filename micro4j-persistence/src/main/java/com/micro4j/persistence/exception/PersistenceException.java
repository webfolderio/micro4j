package com.micro4j.persistence.exception;

import java.sql.SQLException;

public class PersistenceException extends RuntimeException {

    private static final long serialVersionUID = 7464759291754427426L;

    public PersistenceException(SQLException e) {
        super(e);
    }

    public PersistenceException(String message) {
        super(message);
    }
}
