package com.micro4j.mvc;

public interface MvcSession {

    void set(String key, Object value);

    <T> T get(String key);

    boolean contains(String key);

    void remove(String key);
}
