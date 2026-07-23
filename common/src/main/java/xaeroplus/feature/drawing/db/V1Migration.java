package xaeroplus.feature.drawing.db;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaeroplus.XaeroPlus;
import xaeroplus.feature.db.DatabaseMigration;

import java.sql.Connection;
import java.sql.SQLException;

public class V1Migration implements DatabaseMigration {
    @Override
    public boolean shouldMigrate(final String databaseName, final Connection connection, final boolean init) {
        try (var statement = connection.createStatement();
             var resultSet = statement.executeQuery("SELECT version FROM metadata WHERE version = 1")) {
            return !resultSet.next();
        } catch (final Exception e) {
            if (DatabaseMigration.isCorruptDatabase(e)) throw new RuntimeException(e);
            XaeroPlus.LOGGER.error("Failed checking whether {} database needs ellipse migration", databaseName, e);
            return true;
        }
    }

    @Override
    public void doMigration(final String databaseName, final Connection connection, final boolean init) {
        createEllipseTable(databaseName, connection, Level.OVERWORLD);
        createEllipseTable(databaseName, connection, Level.NETHER);
        createEllipseTable(databaseName, connection, Level.END);
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("INSERT OR REPLACE INTO metadata (version) VALUES (1)");
        } catch (final SQLException e) {
            XaeroPlus.LOGGER.error("Failed recording ellipse migration for {} database", databaseName, e);
            throw new RuntimeException(e);
        }
    }

    private void createEllipseTable(final String databaseName, final Connection connection, final ResourceKey<Level> dimension) {
        try (var statement = connection.createStatement()) {
            statement.executeUpdate(
                "CREATE TABLE IF NOT EXISTS \"" + dimension.identifier() + "-ellipses\""
                    + " (centerX INTEGER, centerZ INTEGER, radiusX INTEGER, radiusZ INTEGER, color INTEGER,"
                    + " PRIMARY KEY (centerX, centerZ, radiusX, radiusZ))"
            );
        } catch (final SQLException e) {
            XaeroPlus.LOGGER.error("Error creating ellipses table for db: {}", databaseName, e);
            throw new RuntimeException(e);
        }
    }
}
