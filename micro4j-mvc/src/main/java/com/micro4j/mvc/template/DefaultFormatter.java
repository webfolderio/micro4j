package com.micro4j.mvc.template;

public class DefaultFormatter {

    public void init() {
    }

    public String toString(Object value) {
        return value == null ? "" : value.toString();
    }
}
