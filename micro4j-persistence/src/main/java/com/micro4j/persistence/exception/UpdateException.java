package com.micro4j.persistence.exception;

import java.sql.SQLException;

public class UpdateException extends PersistenceException {

    private static final long serialVersionUID = 4235005692155708275L;

    public UpdateException(SQLException e) {
        super(e);
    }
}
