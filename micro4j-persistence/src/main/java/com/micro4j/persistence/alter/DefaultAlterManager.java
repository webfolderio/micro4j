package com.micro4j.persistence.alter;

import static com.micro4j.persistence.alter.AlterType.CreateColumn;
import static com.micro4j.persistence.alter.AlterType.DropColumn;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.stream.Collectors.toList;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import com.micro4j.persistence.core.ColumnDefinition;
import com.micro4j.persistence.core.DatabaseVendor;
import com.micro4j.persistence.core.PersistenceConfiguration;
import com.micro4j.persistence.core.TableDefinition;
import com.micro4j.persistence.exception.AlterException;
import com.micro4j.persistence.exception.CreateSequenceException;
import com.micro4j.persistence.exception.PersistenceException;
import com.micro4j.persistence.exception.TableCreateException;
import com.micro4j.persistence.exception.TableDropException;
import com.micro4j.persistence.meta.MetaDataManager;

public class DefaultAlterManager implements AlterManager {

    private MetaDataManager metaDataManager;

    private List<AlterListener> listeners = new CopyOnWriteArrayList<>();

    private Marker schemaGeneratorMarker = MarkerFactory.getMarker("REDMOUND-SCHEMA-GENERATOR");

    private PersistenceConfiguration configuration;

    private static final Logger LOG = LoggerFactory.getLogger(DefaultAlterManager.class);

    public DefaultAlterManager(PersistenceConfiguration configuration, MetaDataManager metaDataManager) {
        this.configuration = configuration;
        this.metaDataManager = metaDataManager;
    }

    @Override
    public void create(TableDefinition table) {
        String name = table.getName();

        Optional<TableDefinition> existingTable = metaDataManager.getTable(table.getSchema(), name);

        if (existingTable.isPresent()) {
            throw new AlterException("Table already exist [" + table.getName() + "]");
        }

        List<ColumnDefinition> columns = table.columns();
        if (columns.isEmpty()) {
            throw new AlterException("Missing Columns [" + table.getName() + "]");
        }

        DatabaseVendor vendor = metaDataManager.getVendor();

        String columnQuery = join(", \r\n", columns
                                            .stream()
                                            .map(c -> c.getCreateSql(vendor))
                                            .collect(toList()));

        DataSource ds = configuration.getDataSource();

        boolean hasSchema = metaDataManager.hasSchema(table.getSchema());
        if ( ! hasSchema ) {
            try (Connection conn = ds.getConnection();
                    Statement stmt = conn.createStatement()) {
                String schemaDdl = format("CREATE SCHEMA %s", table.getSchema());
                LOG.info(schemaGeneratorMarker, "Creating schema {}", new Object[] { table.getSchema() });
                LOG.info(schemaGeneratorMarker, "Executing CREATE SCHEMA ddl {}", new Object[] { schemaDdl });
                stmt.executeUpdate(schemaDdl);
            } catch (SQLException e) {
                throw new PersistenceException(e);
            }
        }

        String tableDdl = format("\r\n%s (\r\n%s)", table.generateCreateScript(), columnQuery);

        try (Connection conn = ds.getConnection();
                Statement stmt = conn.createStatement()) {
            LOG.info("Executing CREATE TABLE ddl {}", new Object[] { tableDdl });
            stmt.executeUpdate(tableDdl);
        } catch (SQLException e) {
            throw new TableCreateException(e);
        }

        listeners
            .forEach(listener -> listener.onTableCreate(
                                            new AlterEvent(
                                                        table.getSchema(),
                                                        table.getName())));
    }

    @Override
    public List<Alter> prepareAlter(TableDefinition existingTable, TableDefinition newTable) {
        if ( ! existingTable.getName().equals(newTable.getName())) {
            throw new AlterException("Both table must have same table name. Existing table name [" +
                                                    existingTable.getName() + "] new table name [" + newTable.getName() + "]");
        }

        String existingTableSchema = existingTable.getSchema() == null ? configuration.getDefaultSchema() : existingTable.getSchema();
        String newTableSchema = newTable.getSchema() == null ? configuration.getDefaultSchema() : newTable.getSchema();

        if ( ! newTableSchema.equals(existingTableSchema)) {
            throw new AlterException("Both table must have same schema. Existing table schema [" +
                                                    existingTableSchema + "] new table schema [" + newTableSchema + "]");
        }

        List<Alter> newColumns = newTable
                                        .columns()
                                        .stream()
                                        .filter(nc -> ! existingTable.columnNames().contains(nc.getName()))
                                        .map(nc -> new Alter(nc, CreateColumn))
                                        .collect(toList());

        List<Alter> dropColumns = existingTable
                                        .columns()
                                        .stream()
                                        .filter(ec -> ! newTable.columnNames().contains(ec.getName()))
                                        .map(ec -> new Alter(ec, DropColumn))
                                        .collect(toList());

        newColumns.addAll(dropColumns);

        return newColumns;
    }

    @Override
    public void executeAlter(TableDefinition table, List<Alter> alters) {
        String schema = table.getSchema() == null ? configuration.getDefaultSchema() : table.getSchema();
        DatabaseVendor vendor = metaDataManager.getVendor();

        List<String> alterAdd = alters
                                    .stream()
                                    .filter(alter -> CreateColumn.equals(alter.getType()))
                                    .map(alter -> alter.getColumn().getAlterAdd(table.getName(), schema, vendor))
                                    .collect(toList());

        List<String> alterDrop = alters
                                    .stream()
                                    .filter(alter -> DropColumn.equals(alter.getType()))
                                    .map(alter -> alter.getColumn().getAlterDrop(table.getName(), schema, vendor))
                                    .collect(toList());

        alterAdd.addAll(alterDrop);

        DataSource ds = configuration.getDataSource();

        for (String alter : alterAdd) {
            try (Connection conn = ds.getConnection();
                    Statement stmt = conn.createStatement()) {
                LOG.info(schemaGeneratorMarker, "Executing ALTER ddl {}", new Object[] { alter });
                stmt.executeUpdate(alter);
            } catch (SQLException e) {
                throw new AlterException(e);
            }
        }

        listeners
            .forEach(listener -> listener.onTableAlterColumn(
                                            new AlterEvent(
                                                        table.getSchema(),
                                                        table.getName())));
    }

    @Override
    public void drop(TableDefinition table) {
        DataSource ds = configuration.getDataSource();
        String ddl = table.generateDropScript();
        try (Connection conn = ds.getConnection();
                Statement stmt = conn.createStatement()) {
            LOG.info(schemaGeneratorMarker, "Executing DROP TABLE ddl {}", new Object[] { ddl });
            stmt.executeUpdate(ddl);
        } catch (SQLException e) {
            throw new TableDropException(e);
        }

        listeners
            .forEach(listener -> listener.onTableDrop(
                                            new AlterEvent(
                                                        table.getSchema(),
                                                        table.getName())));
    }


    @Override
    public boolean createSequence(String schema, String sequence) {
        DataSource ds = configuration.getDataSource();
        try (Connection conn = ds.getConnection();
                Statement stmt = conn.createStatement()) {
            String ddl = format("create sequence %s.%s", schema, sequence);
            LOG.info(schemaGeneratorMarker, "Executing CREATE SEQUENCE ddl {}", new Object[] { ddl });
            return stmt.executeUpdate(ddl) > 0;
        } catch (SQLException e) {
            throw new CreateSequenceException("Unable to create sequence " + schema + "." + sequence);
        }
    }

    @Override
    public boolean addListener(AlterListener listener) {
        if ( ! listeners.contains(listener) && listener != null ) {
            return listeners.add(listener);
        }
        return false;
    }

    @Override
    public boolean removeListener(AlterListener listener) {
        if ( listener != null) {
            return listeners.remove(listener);
        }
        return false;
    }

    public void onShutdown() {
        listeners.clear();
    }
}
