/* MIT License
 * 
 * Copyright (c) 2016 http://micro4j.com
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.micro4j.persistence.manager;

import static java.lang.Character.isUpperCase;
import static java.lang.Character.toLowerCase;
import static java.lang.Character.toUpperCase;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

class Utils {

    private ConcurrentMap<String, String> camelCaseToTableName = new ConcurrentHashMap<>();

    private ConcurrentMap<String, String> tableNameToCamelCase = new ConcurrentHashMap<>();

    public String camelCaseToTableName(String name) {
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

    public String tableNameToCamelCase(String name) {
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

}
