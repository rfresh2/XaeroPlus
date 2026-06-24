package xaeroplus.feature.highlights.db;

import xaeroplus.XaeroPlus;
import xaeroplus.feature.db.DatabaseMigration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static xaeroplus.feature.db.DatabaseMigration.executeCancellable;

public class V1ToV2Migration implements DatabaseMigration {
    private static final int VERSION = 2;

    @Override
    public boolean shouldMigrate(final String databaseName, final Connection connection) throws SQLException {
        try {
            if (getMetadataVersion(connection) >= VERSION) return false;
            return !getHighlightTableNames(connection).isEmpty();
        } catch (SQLException ex) {
            throw ex;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void doMigration(final String databaseName, final Connection connection) throws SQLException, InterruptedException {
        for (var tableName : getHighlightTableNames(connection)) {
            if (isWithoutRowid(tableName, connection)) continue;
            migrateTable(databaseName, connection, tableName);
        }
        setMetadataVersion(connection);
    }

    private List<String> getHighlightTableNames(final Connection connection) throws SQLException {
        var tableNames = new ArrayList<String>();
        try (var statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                 "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'"
             )) {
            while (resultSet.next()) {
                var tableName = resultSet.getString("name");
                if ("metadata".equals(tableName)) continue;
                if (tableName.endsWith("_v2_migration")) continue;
                if (isHighlightTable(tableName, connection)) {
                    tableNames.add(tableName);
                }
            }
        }
        return tableNames;
    }

    private boolean isHighlightTable(final String tableName, final Connection connection) throws SQLException {
        var hasX = false;
        var hasZ = false;
        var hasFoundTime = false;
        try (var statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery("PRAGMA table_info(" + quoteIdentifier(tableName) + ")");
            while (resultSet.next()) {
                switch (resultSet.getString("name")) {
                    case "x" -> hasX = true;
                    case "z" -> hasZ = true;
                    case "foundTime" -> hasFoundTime = true;
                    default -> {
                    }
                }
            }
        }
        return hasX && hasZ && hasFoundTime;
    }

    private void migrateTable(final String databaseName, final Connection connection, final String tableName) throws SQLException, InterruptedException {
        var newTableName = tableName + "_v2_migration";
        XaeroPlus.LOGGER.info("Migrating {} database table {} to V2", databaseName, tableName);
        executeCancellable(connection, "DROP TABLE IF EXISTS " + quoteIdentifier(newTableName));
        executeCancellable(connection, "CREATE TABLE " + quoteIdentifier(newTableName)
            + " (x INTEGER, z INTEGER, foundTime INTEGER, PRIMARY KEY (x, z)) WITHOUT ROWID");
        executeCancellable(connection, "INSERT INTO " + quoteIdentifier(newTableName)
            + " (x, z, foundTime) SELECT x, z, foundTime FROM " + quoteIdentifier(tableName));
        executeCancellable(connection, "DROP TABLE " + quoteIdentifier(tableName));
        executeCancellable(connection, "ALTER TABLE " + quoteIdentifier(newTableName) + " RENAME TO " + quoteIdentifier(tableName));
    }

    private int getMetadataVersion(final Connection connection) throws SQLException {
        if (!tableExists("metadata", connection)) return 0;
        try (var statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT version FROM metadata WHERE id = 0")) {
            if (resultSet.next()) {
                return resultSet.getInt("version");
            }
        }
        return 0;
    }

    private void setMetadataVersion(final Connection connection) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS metadata (id INTEGER PRIMARY KEY, version INTEGER)");
            statement.executeUpdate("INSERT OR REPLACE INTO metadata (id, version) VALUES (0, " + VERSION + ")");
        }
    }

    private boolean tableExists(final String tableName, final Connection connection) throws SQLException {
        try (var statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                 "SELECT name FROM sqlite_master WHERE type = 'table' AND name = " + quoteLiteral(tableName)
             )) {
            return resultSet.next();
        }
    }

    private boolean isWithoutRowid(final String tableName, final Connection connection) throws SQLException {
        try (var statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                 "SELECT sql FROM sqlite_master WHERE type = 'table' AND name = " + quoteLiteral(tableName)
             )) {
            return resultSet.next()
                && resultSet.getString("sql") != null
                && resultSet.getString("sql").toUpperCase(Locale.ROOT).contains("WITHOUT ROWID");
        }
    }

    private String quoteIdentifier(final String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    private String quoteLiteral(final String literal) {
        return "'" + literal.replace("'", "''") + "'";
    }
}
