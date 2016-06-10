package com.micro4j.persistence.exception;

import java.sql.SQLException;

public class QueryException extends PersistenceException {

    private static final long serialVersionUID = -7661076737905743698L;

    public QueryException(SQLException e) {
        super(e);
    }
}
