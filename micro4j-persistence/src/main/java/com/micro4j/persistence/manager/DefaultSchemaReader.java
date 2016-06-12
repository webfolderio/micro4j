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

import static org.w3c.dom.Node.ELEMENT_NODE;

import java.io.IOException;
import java.io.InputStream;
import java.sql.JDBCType;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.micro4j.persistence.core.ColumnDefinition;
import com.micro4j.persistence.core.SchemaDefinition;
import com.micro4j.persistence.core.TableDefinition;
import com.micro4j.persistence.exception.SchemaParseException;

public class DefaultSchemaReader implements SchemaReader {

    @Override
    public SchemaDefinition read(InputStream is) {
        Document document = parse(is);
        Element schema = document.getDocumentElement();
        if ( ! schema.getTagName().equals("schema") ) {
            throw new SchemaParseException("Missing schema tag");
        }
        String schemaName = schema.getAttribute("name");
        if (schemaName == null || schemaName.trim().isEmpty()) {
            throw new SchemaParseException("schema name is required");
        }
        String defaultSequence = schema.getAttribute("sequence");
        if ( defaultSequence != null &&  defaultSequence.trim().isEmpty() ) {
            defaultSequence = null;
        }
        List<TableDefinition> definitions = new ArrayList<>();
        List<Element> tables = getChildNodes(schema, "table");
        for (Element table : tables) {
            String tableName = table.getAttribute("name");
            TableDefinition tableDefinition = new TableDefinition(schemaName, tableName);
            List<Element> columns = getChildNodes(table, "column");
            if (columns.isEmpty()) {
                throw new SchemaParseException("table [" + tableName + "] must have at least one column");
            }
            for (Element column : columns) {
                // ----
                // name
                // ----
                String columnName = column.getAttribute("name");
                if (columnName == null || columnName.trim().isEmpty()) {
                    throw new SchemaParseException("Table [" + tableName + "] column has no name attribute");
                }
                ColumnDefinition columnDefinition = new ColumnDefinition(columnName);
                // ----
                // type
                // ----
                String type = column.getAttribute("type");
                if (type == null || type.trim().isEmpty()) {
                    throw new SchemaParseException("Table [" + tableName + "] column [" + columnName + "] has no type attribute");
                }
                JDBCType jdbcType = JDBCType.valueOf(type.trim());
                columnDefinition.setJdbcType(jdbcType);
                // ----
                // size
                // ----
                String size = column.getAttribute("size");
                if ( size != null && ! size.trim().isEmpty() ) {
                    int columnSize = Integer.parseInt(size);
                    if (columnSize <= 0) {
                        throw new SchemaParseException("Table [" + tableName + "] column [" + columnName + "] size attribute must be positive integer");
                    }
                    columnDefinition.setSize(columnSize);
                }
                // -----
                // scale
                // -----
                String scale = column.getAttribute("scale");
                if ( scale != null && ! scale.trim().isEmpty() ) {
                    int columnScale = Integer.parseInt(scale);
                    if (columnScale < 0) {
                        throw new SchemaParseException("Table [" + tableName + "] column [" + columnName + "] scale attribute must be positive integer");
                    }
                    columnDefinition.setScale(columnScale);
                }
                // --------
                // sequence
                // --------
                columnDefinition.setSequence(defaultSequence);
                String sequence = column.getAttribute("sequence");
                if ( sequence != null && sequence.trim().isEmpty() ) {
                    columnDefinition.setSequence(sequence);
                }
                // --------
                // nullable
                // --------
                String nullable = column.getAttribute("nullable");
                if ( nullable != null && ! nullable.trim().isEmpty() ) {
                    columnDefinition.setNullable(Boolean.parseBoolean(nullable));
                }
                // -----------
                // primary key
                // -----------
                String primaryKey = column.getAttribute("primaryKey");
                if ( primaryKey != null && ! primaryKey.trim().isEmpty() ) {
                    columnDefinition.setPrimaryKey(Boolean.parseBoolean(primaryKey));
                }
                tableDefinition.add(columnDefinition);
            }
            definitions.add(tableDefinition);
        }
        return new SchemaDefinition(schemaName, defaultSequence, definitions);
    }

    protected Document parse(InputStream is) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document document = null;
        try {
            DocumentBuilder documentBuilder = factory.newDocumentBuilder();
            document = documentBuilder.parse(is);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new SchemaParseException(e);
        }
        return document;
    }

    protected List<Element> getChildNodes(Element element, String tagName) {
        NodeList nodes = element.getChildNodes();
        List<Element> elements = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node != null && ELEMENT_NODE == node.getNodeType()) {
                Element next = (Element) node;
                elements.add(next);
            }
        }
        return elements;
    }
}
