package com.micro4j.mvc;

public class ViewModel<T> {

    private String name;

    private String container;

    private T entity;

    public ViewModel(String name, T entity) {
        this(name, null, entity);
    }

    public ViewModel(String name, String container, T entity) {
        this.name = name;
        this.container = container;
        this.entity = entity;
    }

    public String getName() {
        return name;
    }

    public T getEntity() {
        return entity;
    }

    public String getContainer() {
        return container;
    }

    @Override
    public String toString() {
        return "ViewModel [name=" + name + ", container=" + container + ", entity=" + entity + "]";
    }
}
