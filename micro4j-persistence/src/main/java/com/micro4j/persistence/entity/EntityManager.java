package com.micro4j.persistence.entity;

import java.util.List;
import java.util.Map;

public interface EntityManager {

    long insert(String entityName, Map<String, Object> entity);

    long insertSelective(String entityName, Map<String, Object> entity);

    Map<String, Object> get(String entityName, long id);

    List<Map<String, Object>> listAll(String entityName);

    boolean update(String entityName, Map<String, Object> entity);

    boolean updateSelective(String entityName, Map<String, Object> entity);

    boolean delete(String entityName, long id);
}
