package com.micro4j.persistence.exception;

import java.sql.SQLException;

public class TableDropException extends PersistenceException {

    private static final long serialVersionUID = -7865523992885466292L;

    public TableDropException(SQLException e) {
        super(e);
    }
}
