package com.micro4j.mvc;

import java.io.IOException;

public class MvcException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public MvcException(IOException e) {
        super(e);
    }

    public MvcException(String message) {
        super(message);
    }
}
