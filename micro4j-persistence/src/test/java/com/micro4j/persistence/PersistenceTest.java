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

import static java.sql.JDBCType.BIGINT;
import static java.sql.JDBCType.CHAR;
import static java.sql.JDBCType.DECIMAL;
import static java.sql.JDBCType.TIMESTAMP;
import static java.sql.JDBCType.VARCHAR;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.runners.MethodSorters.NAME_ASCENDING;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.hsqldb.jdbc.JDBCDataSource;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;

import com.micro4j.persistence.core.Alter;
import com.micro4j.persistence.core.AlterManager;
import com.micro4j.persistence.core.AlterType;
import com.micro4j.persistence.core.ColumnDefinition;
import com.micro4j.persistence.core.PersistenceConfiguration;
import com.micro4j.persistence.core.TableDefinition;
import com.micro4j.persistence.manager.DefaultEntityManager;
import com.micro4j.persistence.manager.EntityManager;
import com.micro4j.persistence.manager.MetaDataManager;

@FixMethodOrder(NAME_ASCENDING)
public class PersistenceTest {

    private static MetaDataManager metaDataManager;
    private static EntityManager entityManager;
    private static AlterManager alterManager;

    @BeforeClass
    public static void before() {
        JDBCDataSource dataSource = new JDBCDataSource();
        dataSource.setUrl("jdbc:hsqldb:mem:mydb");
        dataSource.setUser("sa");
        dataSource.setPassword("");
        PersistenceConfiguration configuration = new PersistenceConfiguration("MYSCHEMA", "MY_SEQ", dataSource);
        entityManager = new DefaultEntityManager(configuration);
        alterManager = entityManager.getAlterManager();
        metaDataManager = entityManager.getMetaDataManager();
    }

    @Test
    public void t01Crud() {
        TableDefinition table = new TableDefinition("MYSCHEMA", "SAMPLE_ENTITY");
        table.add(new ColumnDefinition("CREATED_BY").setSize(255).setJdbcType(VARCHAR));
        table.add(new ColumnDefinition("SURNAME").setSize(255).setJdbcType(VARCHAR).setNullable(true));
        table.add(new ColumnDefinition("CREATE_DATE").setJdbcType(TIMESTAMP));
        table.add(new ColumnDefinition("ACTIVE").setSize(1).setJdbcType(CHAR));
        table.add(new ColumnDefinition("UPDATE_DATE").setJdbcType(TIMESTAMP));
        table.add(new ColumnDefinition("UPDATED_BY").setSize(255).setJdbcType(VARCHAR));    
        table.add(new ColumnDefinition("ID").setJdbcType(BIGINT).setSequence("MY_SEQ").setPrimaryKey(true));
        table.add(new ColumnDefinition("NAME").setSize(255).setJdbcType(VARCHAR).setNullable(true));
        table.add(new ColumnDefinition("AGE").setSize(10).setScale(2).setJdbcType(DECIMAL).setNullable(true));

        alterManager.create(table);

        alterManager.createSequence("MYSCHEMA", "MY_SEQ");

        Map<String, Object> entity = new HashMap<>();
        entity.put("NAME", "foo");
        entity.put("SURNAME", "bar");

        long id = entityManager.insert("SAMPLE_ENTITY", entity);
        Map<String, Object> map = entityManager.get("SAMPLE_ENTITY", id);
        assertEquals("1", map.get("ACTIVE"));

        List<Map<String, Object>> records = entityManager.listAll("SAMPLE_ENTITY");
        assertEquals(1, records.size());
        
        assertEquals("foo", map.get("NAME"));
        assertEquals("bar", map.get("SURNAME"));

        assertTrue(entityManager.delete("SAMPLE_ENTITY", id));

        map = entityManager.get("SAMPLE_ENTITY", id);
        assertEquals("0", map.get("ACTIVE"));

        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put("ID", map.get("ID"));
        updateMap.put("NAME", "foo-1");
        updateMap.put("SURNAME", "bar-1");

        assertTrue(entityManager.update("SAMPLE_ENTITY", updateMap));

        entity = entityManager.get("SAMPLE_ENTITY", (long) updateMap.get("ID"));

        assertEquals("foo-1", entity.get("NAME"));
        assertEquals("bar-1", entity.get("SURNAME"));
    }

    @Test
    public void t02metaDataManager() {
        assertEquals(1, metaDataManager.getTables().size());

        assertTrue(metaDataManager.getTables().contains("SAMPLE_ENTITY"));

        TableDefinition table = metaDataManager.getTable("MYSCHEMA", "SAMPLE_ENTITY").get();

        ColumnDefinition idColumn = table.getColumn("ID");

        assertFalse(idColumn.isNullable());

        assertTrue(idColumn.isPrimaryKey());

        ColumnDefinition createDate = table.getColumn("CREATE_DATE");

        assertFalse(createDate.isNullable());

        assertEquals(9, table.columnNames().size());

        assertTrue(table.columnNames().containsAll(asList(
                    "ID",
                    "CREATE_DATE",
                    "UPDATE_DATE",
                    "CREATED_BY",
                    "UPDATED_BY",
                    "ACTIVE",
                    "NAME",
                    "SURNAME",
                    "AGE")
        ));
    }

    @Test
    public void t03testCreateTable() {
        TableDefinition newTable = new TableDefinition("MYSCHEMA", "MYTABLE");

        newTable.add(new ColumnDefinition("NAME", 255, 0, VARCHAR, false, false, null));

        alterManager.create(newTable);

        TableDefinition createdTable = metaDataManager.getTable("MYSCHEMA", "MYTABLE").get();

        assertEquals(newTable.getName(), createdTable.getName());
        ColumnDefinition column = createdTable.getColumn("NAME");
        assertNotNull(column);

        assertEquals(255, column.getSize());
        assertEquals(0, column.getScale());
        assertEquals(VARCHAR, column.getJdbcType());
    }

    @Test
    public void t04testDropTable() {
        TableDefinition mytable = new TableDefinition("MYSCHEMA", "MYTABLE");
        alterManager.drop(mytable);
        Optional<TableDefinition> found = metaDataManager.getTable("MYTABLE");
        assertFalse(found.isPresent());
    }

    @Test
    public void t05prepareAlter() {
        TableDefinition newTable = new TableDefinition("MYFOO");
        newTable.add(new ColumnDefinition("NEWCOLUMN", 10, 2, VARCHAR, false, false, null));

        TableDefinition existingTable = new TableDefinition("MYFOO");
        existingTable.add(new ColumnDefinition("OLDCOLUMN", 10, 2, VARCHAR, false, false, null));

        List<Alter> list = alterManager.prepareAlter(existingTable, newTable);

        assertEquals(2, list.size());

        Alter firstAlter = list.get(0);

        assertEquals(AlterType.CreateColumn, firstAlter.getType());
        assertEquals("NEWCOLUMN", firstAlter.getColumn().getName());

        Alter secondAlter = list.get(1);

        assertEquals(AlterType.DropColumn, secondAlter.getType());
        assertEquals("OLDCOLUMN", secondAlter.getColumn().getName());
    }

    @Test
    public void t06executeAlter() {
        TableDefinition existingTable = metaDataManager.getTable("MYSCHEMA", "SAMPLE_ENTITY").get();

        TableDefinition newTable = new TableDefinition(existingTable);
        newTable.removeColumn("SURNAME");
        newTable.add(new ColumnDefinition("NEWCOLUMN", 10, 0, VARCHAR, false, true, null));

        List<Alter> alter = alterManager.prepareAlter(existingTable, newTable);

        alterManager.executeAlter(newTable, alter);

        TableDefinition createdTable = metaDataManager.getTable("MYSCHEMA", "SAMPLE_ENTITY").get();
        ColumnDefinition newColumn = createdTable.getColumn("NEWCOLUMN");
        assertNotNull(newColumn);

        assertTrue(createdTable.getColumn("NEWCOLUMN").isNullable());
        
        assertEquals(10, newColumn.getSize());

        assertFalse(createdTable.hasColumn("SURNAME"));
    }

    @Test
    public void t07listSequences() {
        assertTrue(metaDataManager.listSequences("MYSCHEMA").contains("MY_SEQ"));   

        entityManager.dispose();
    }
}
