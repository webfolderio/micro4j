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
package com.micro4j.persistence;

import static org.junit.Assert.assertEquals;

import java.sql.JDBCType;
import java.util.List;

import org.hsqldb.jdbc.JDBCDataSource;
import org.junit.Test;

import com.micro4j.persistence.core.ColumnDefinition;
import com.micro4j.persistence.core.PersistenceConfiguration;
import com.micro4j.persistence.core.SchemaDefinition;
import com.micro4j.persistence.core.TableDefinition;
import com.micro4j.persistence.manager.DefaultEntityManager;
import com.micro4j.persistence.manager.DefaultSchemaReader;
import com.micro4j.persistence.manager.EntityManager;
import com.micro4j.persistence.manager.SchemaReader;

public class SchemaReaderTest {

    @Test
    public void test() {
        SchemaReader reader = new DefaultSchemaReader();
        SchemaDefinition schema = reader.read(SchemaReaderTest.class.getClassLoader().getResourceAsStream("my-schema.xml"));
        List<TableDefinition> tables = schema.getDefinitions();

        assertEquals(1, tables.size());

        TableDefinition table = tables.get(0);

        assertEquals("MY_SCHEMA", table.getSchema());
        assertEquals("MY_TABLE", table.getName());

        List<ColumnDefinition> columns = table.columns();

        assertEquals(7, columns.size());
        assertEquals("ID", columns.get(0).getName());
        assertEquals(JDBCType.BIGINT, columns.get(0).getJdbcType());
        assertEquals(true, columns.get(0).isPrimaryKey());
        assertEquals("ACTIVE", columns.get(1).getName());
        assertEquals("CREATE_DATE", columns.get(2).getName());
        assertEquals(JDBCType.TIMESTAMP, columns.get(2).getJdbcType());
        assertEquals("UPDATE_DATE", columns.get(3).getName());
        assertEquals(JDBCType.TIMESTAMP, columns.get(3).getJdbcType());
        assertEquals("CREATED_BY", columns.get(4).getName());
        assertEquals(JDBCType.VARCHAR, columns.get(4).getJdbcType());
        assertEquals(32, columns.get(4).getSize());
        assertEquals("UPDATED_BY", columns.get(5).getName());
        assertEquals(JDBCType.VARCHAR, columns.get(5).getJdbcType());
        assertEquals(32, columns.get(5).getSize());
        assertEquals("NAME", columns.get(6).getName());
        assertEquals(JDBCType.VARCHAR, columns.get(6).getJdbcType());
        assertEquals(255, columns.get(6).getSize());
        assertEquals(true, columns.get(6).isNullable());

        JDBCDataSource dataSource = new JDBCDataSource();
        dataSource.setURL("jdbc:hsqldb:mem:mydb");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        EntityManager manager = new DefaultEntityManager(new PersistenceConfiguration(schema.getName(), schema.getSequence(), dataSource));
        manager.getAlterManager().create(schema);

        SchemaDefinition schemaAlter = reader.read(SchemaReaderTest.class.getClassLoader().getResourceAsStream("my-schema-alter.xml"));
        manager.getAlterManager().create(schemaAlter);
    }
}
