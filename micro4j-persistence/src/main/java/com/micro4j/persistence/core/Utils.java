package com.micro4j.persistence.core;

import static java.lang.Character.isUpperCase;
import static java.lang.Character.toLowerCase;
import static java.lang.Character.toUpperCase;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Utils {

    private static ConcurrentMap<String, String> camelCaseToTableName = new ConcurrentHashMap<>();

    private static ConcurrentMap<String, String> tableNameToCamelCase = new ConcurrentHashMap<>();

    private static ConcurrentMap<String, String> tableNameToTitleCase = new ConcurrentHashMap<>();

    private static ConcurrentMap<String, String> camelCaseToTitleCase = new ConcurrentHashMap<>();

    public static String camelCaseToTableName(String name) {
        if (name == null) {
            return null;
        }
        String tableName = camelCaseToTableName.get(name);
        if (tableName != null) {
            return tableName;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (isUpperCase(c) && i != 0) {
                builder.append("_");
            }
            builder.append(toUpperCase(c));
        }
        tableName = builder.toString();
        camelCaseToTableName.putIfAbsent(name, tableName);
        return tableName;
    }

    public static String tableNameToCamelCase(String name) {
        if (name == null) {
            return null;
        }
        String camelCase = tableNameToCamelCase.get(name);
        if (camelCase != null) {
            return camelCase;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (i == 0 || (isUpperCase(c) &&
                        (i > 0 && name.charAt(i - 1) != '_'))) {
                builder.append(toLowerCase(c));
            } else if (c != '_') {
                builder.append(c);
            }
        }
        camelCase = builder.toString();
        tableNameToCamelCase.putIfAbsent(name, camelCase);
        return camelCase;
    }

    public static String tableNameToTitleCase(String name) {
        if (name == null) {
            return null;
        }
        String titleCase = tableNameToTitleCase.get(name);
        if (titleCase != null) {
            return titleCase;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (i == 0) {
                builder.append(toUpperCase(c));
            } else if (isUpperCase(c) &&
                        (i > 0 && name.charAt(i - 1) != '_')) {
                builder.append(toLowerCase(c));
            } else if (c == '_') {
                // no op
            } else {
                builder.append(c);
            }
        }
        titleCase = builder.toString();
        tableNameToTitleCase.putIfAbsent(name, titleCase);
        return titleCase;
    }

    public static String camelCaseToTitleCase(String name) {
        if (name == null) {
            return null;
        }
        String titleCase = camelCaseToTitleCase.get(name);
        if (titleCase != null) {
            return titleCase;
        }
        titleCase = toUpperCase(name.charAt(0)) + name.substring(1);
        camelCaseToTitleCase.putIfAbsent(name, titleCase);
        return titleCase;
    }
}
