package xaeroplus.feature.drawing.db;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaeroplus.XaeroPlus;
import xaeroplus.feature.db.DatabaseMigration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public class V0Migration implements DatabaseMigration {
    @Override
    public boolean shouldMigrate(final String databaseName, final Connection connection) {
        try {
            if (!tableExists("metadata", connection)) return true;
            try (var statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery("SELECT version FROM metadata where version = 0");
                if (!resultSet.next()) {
                    return true;
                }
            }
        } catch (Exception e) {
            XaeroPlus.LOGGER.error("Failed checking if {} database should migrate", databaseName, e);
        }
        return false;
    }

    @Override
    public void doMigration(final String databaseName, final Connection connection) {
        createMetadataTable(databaseName, connection);
        createLinesTable(databaseName, connection, Level.OVERWORLD);
        createLinesTable(databaseName, connection, Level.NETHER);
        createLinesTable(databaseName, connection, Level.END);
        createHighlightsTable(databaseName, connection, Level.OVERWORLD);
        createHighlightsTable(databaseName, connection, Level.NETHER);
        createHighlightsTable(databaseName, connection, Level.END);
        createTextsTable(databaseName, connection, Level.OVERWORLD);
        createTextsTable(databaseName, connection, Level.NETHER);
        createTextsTable(databaseName, connection, Level.END);
    }

    private void createHighlightsTable(final String databaseName, final Connection connection, final ResourceKey<Level> dimension) {
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS \"" + getTableName(dimension, "highlights") + "\" (x INTEGER, z INTEGER, color INTEGER, PRIMARY KEY (x, z))");
        } catch (SQLException e) {
            XaeroPlus.LOGGER.error("Error creating highlights table for db: {}", databaseName, e);
            throw new RuntimeException(e);
        }
    }

    private void createLinesTable(final String databaseName, final Connection connection, ResourceKey<Level> dimension) {
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS \"" + getTableName(dimension, "lines") + "\" (x1 INTEGER, z1 INTEGER, x2 INTEGER, z2 INTEGER, color INTEGER, PRIMARY KEY (x1, z1, x2, z2))");
        } catch (SQLException e) {
            XaeroPlus.LOGGER.error("Error creating lines table for db: {}", databaseName, e);
            throw new RuntimeException(e);
        }
    }

    private void createTextsTable(final String databaseName, final Connection connection, ResourceKey<Level> dimension) {
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS \"" + getTableName(dimension, "texts") + "\" (value TEXT, x INTEGER, z INTEGER, color INTEGER, scale REAL, PRIMARY KEY (x, z))");
        } catch (SQLException e) {
            XaeroPlus.LOGGER.error("Error creating texts table for db: {}", databaseName, e);
            throw new RuntimeException(e);
        }
    }

    private String getTableName(ResourceKey<Level> dimension, String type) {
        return dimension.location().toString() + "-" + type;
    }

    private void createMetadataTable(String databaseName, Connection connection) {
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS metadata (version INTEGER PRIMARY KEY, time DATETIME NOT NULL default CURRENT_TIMESTAMP)");
            statement.executeUpdate("INSERT OR REPLACE INTO metadata (version) VALUES (0)");
        } catch (SQLException e) {
            XaeroPlus.LOGGER.error("Error creating metadata table for db: {}", databaseName, e);
            throw new RuntimeException(e);
        }
    }

    private boolean tableExists(String tableName, Connection connection) throws SQLException {
        try (var statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery("SELECT name FROM sqlite_master WHERE type='table' and name='" + tableName + "'");
            return resultSet.next();
        }
    }
}
