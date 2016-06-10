package com.micro4j.persistence.exception;

import java.sql.SQLException;

public class AlterException extends PersistenceException {

    private static final long serialVersionUID = -8434200761211936335L;

    public AlterException(SQLException e) {
        super(e);
    }

    public AlterException(String message) {
        super(message);
    }
}
