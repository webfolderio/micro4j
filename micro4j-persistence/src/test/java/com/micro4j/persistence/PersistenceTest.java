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

import java.sql.JDBCType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.hsqldb.jdbc.JDBCDataSource;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;

import com.micro4j.persistence.alter.Alter;
import com.micro4j.persistence.alter.AlterType;
import com.micro4j.persistence.alter.DefaultAlterManager;
import com.micro4j.persistence.core.ColumnDefinition;
import com.micro4j.persistence.core.PersistenceConfiguration;
import com.micro4j.persistence.core.TableDefinition;
import com.micro4j.persistence.entity.DefaultEntityManager;
import com.micro4j.persistence.meta.DefaultMetaDataManager;

@FixMethodOrder(NAME_ASCENDING)
public class PersistenceTest {

    private static DefaultMetaDataManager metaDataManager;
    private static DefaultEntityManager entityManager;
    private static DefaultAlterManager alterManager;

    @BeforeClass
    public static void before() {
        JDBCDataSource dataSource = new JDBCDataSource();
        dataSource.setUrl("jdbc:hsqldb:mem:mydb");
        dataSource.setUser("sa");
        dataSource.setPassword("");
        PersistenceConfiguration configuration = new PersistenceConfiguration("MYSCHEMA", "MY_SEQ", dataSource);
        metaDataManager = new DefaultMetaDataManager(configuration);
        entityManager = new DefaultEntityManager(configuration);
        alterManager = new DefaultAlterManager(configuration, metaDataManager);
    }

    @Test
    public void t01_entityManager() {
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
        entity.put("name", "foo");
        entity.put("surname", "bar");

        long id = entityManager.insert("SampleEntity", entity);
        Map<String, Object> map = entityManager.get("SampleEntity", id);
        assertEquals("1", map.get("active"));

        assertEquals("foo", map.get("name"));
        assertEquals("bar", map.get("surname"));

        assertTrue(entityManager.delete("SampleEntity", id));

        map = entityManager.get("SampleEntity", id);
        assertEquals("0", map.get("active"));


        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put("id", map.get("id"));
        updateMap.put("name", "foo-1");
        updateMap.put("surname", "bar-1");

        assertTrue(entityManager.update("SampleEntity", updateMap));

        entity = entityManager.get("SampleEntity", (long) updateMap.get("id"));

        assertEquals("foo-1", entity.get("name"));
        assertEquals("bar-1", entity.get("surname"));
    }

    @Test
    public void t02_metaDataManager() {
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
    public void t03_testCreateTable() {
        TableDefinition newTable = new TableDefinition("MYSCHEMA", "MYTABLE");

        newTable.add(new ColumnDefinition("NAME", 255, 0, VARCHAR, false, false, null));

        alterManager.create(newTable);

        TableDefinition createdTable = metaDataManager.getTable("MYSCHEMA", "MYTABLE").get();

        assertEquals(newTable.getName(), createdTable.getName());
        ColumnDefinition column = createdTable.getColumn("NAME");
        assertNotNull(column);

        assertEquals(255, column.getSize());
        assertEquals(0, column.getScale());
        assertEquals(JDBCType.VARCHAR, column.getJdbcType());
    }

    @Test
    public void t04_testDropTable() {
        TableDefinition mytable = new TableDefinition("MYSCHEMA", "MYTABLE");
        alterManager.drop(mytable);
        Optional<TableDefinition> found = metaDataManager.getTable("MYTABLE");
        assertFalse(found.isPresent());
    }

    @Test
    public void t05_prepareAlter() {
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
    public void t06_executeAlter() {
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
    public void t07_listSequences() {
        assertTrue(metaDataManager.listSequences("MYSCHEMA").contains("MY_SEQ"));   
    }
}
