package xaeroplus.feature.render.highlights.db;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaeroplus.XaeroPlus;

import java.sql.Connection;

public class V0ToV1Migration implements DatabaseMigration {

    /**
     * Migration tasks:
     *  1. Migrate from int indexed dimension ID's to stringified resource keys
     *  2. Add DB metadata table with version
     */

    @Override
    public boolean shouldMigrate(String databaseName, Connection connection) {
        try (var statement = connection.createStatement()) {
            // list all tables
            try (var resultSet = statement.executeQuery("SELECT * FROM sqlite_master")) {
                // check if the metadata table exists
                while (resultSet.next()) {
                    if (resultSet.getString("name").equals("metadata")) {
                        return false;
                    }
                }
            }
            // check if the old tables exist
            try (var resultSet = statement.executeQuery("SELECT * FROM sqlite_master")) {
                while (resultSet.next()) {
                    if (resultSet.getString("name").equals("0") || resultSet.getString("name").equals("1") || resultSet.getString("name").equals("-1")) {
                        return true;
                    }
                }
            }
            // no old tables found
            return false;
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Failed performing {} database migration", databaseName, e);
            return false;
        }
    }

    @Override
    public void doMigration(String databaseName, final Connection connection) {
        // migrate old tables and indexes
        try {
            try (var statement = connection.createStatement()) {
                // migrate table name
                statement.executeUpdate("ALTER TABLE \"0\" RENAME TO \"" + getTableName(Level.OVERWORLD) + "\"");
                statement.executeUpdate("ALTER TABLE \"-1\" RENAME TO \"" + getTableName(Level.NETHER) + "\"");
                statement.executeUpdate("ALTER TABLE \"1\" RENAME TO \"" + getTableName(Level.END) + "\"");
            }

            try (var statement = connection.createStatement()) {
                // rebuild new indexes
                statement.executeUpdate("CREATE UNIQUE INDEX IF NOT EXISTS \"unique_xz_" + getTableName(Level.OVERWORLD) + "\" ON \"" + getTableName(Level.OVERWORLD) + "\" (x, z)");
                statement.executeUpdate("CREATE UNIQUE INDEX IF NOT EXISTS \"unique_xz_" + getTableName(Level.NETHER) + "\" ON \"" + getTableName(Level.NETHER) + "\" (x, z)");
                statement.executeUpdate("CREATE UNIQUE INDEX IF NOT EXISTS \"unique_xz_" + getTableName(Level.END) + "\" ON \"" + getTableName(Level.END) + "\" (x, z)");

                // drop old indexes
                statement.executeUpdate("DROP INDEX IF EXISTS unique_xzO");
                statement.executeUpdate("DROP INDEX IF EXISTS unique_xzN");
                statement.executeUpdate("DROP INDEX IF EXISTS unique_xzE");
            }
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Failed creating new tables for {} database", databaseName, e);
        }
    }

    private String getTableName(ResourceKey<Level> dimension) {
        return dimension.location().toString();
    }
}
