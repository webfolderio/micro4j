package com.micro4j.persistence.exception;

import java.sql.SQLException;

public class DeleteException extends PersistenceException {

    private static final long serialVersionUID = -1273232022715990855L;

    public DeleteException(SQLException e) {
        super(e);
    }
}
