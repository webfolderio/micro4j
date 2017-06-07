package com.micro4j.mvc.csrf;

import com.micro4j.mvc.MvcException;

public class InvalidCsrfTokenException extends MvcException {

    private static final long serialVersionUID = 1L;

    public InvalidCsrfTokenException() {
        super("Invalid csrf token");
    }
}
