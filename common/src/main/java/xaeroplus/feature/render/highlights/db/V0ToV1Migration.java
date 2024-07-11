package xaeroplus.feature.render.highlights.db;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaeroplus.XaeroPlus;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public class V0ToV1Migration implements DatabaseMigration {

    /**
     * Migration tasks:
     *  1. Migrate from int indexed dimension ID's to stringified resource keys
     *  2. Add DB metadata table with version
     */

    @Override
    public boolean shouldMigrate(String databaseName, Connection connection) {
        try {
            return tableExists("0", connection)
                || tableExists("-1", connection)
                || tableExists("1", connection);
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Failed checking if {} database should migrate", databaseName, e);
            return false;
        }
    }

    @Override
    public void doMigration(String databaseName, final Connection connection) {
        // migrate old tables and indexes
        try {
            // migrate table name
            mergeTables("0", getTableName(Level.OVERWORLD), connection);
            mergeTables("-1", getTableName(Level.NETHER), connection);
            mergeTables("1", getTableName(Level.END), connection);

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

    private boolean tableExists(String tableName, Connection connection) throws SQLException {
        try (var statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery("SELECT name FROM sqlite_master WHERE type='table' and name='" + tableName + "'");
            return resultSet.next();
        }
    }

    private void mergeTables(String src, String dest, Connection connection) throws SQLException {
        try (var statement = connection.createStatement()) {
            // migrate table name
            if (tableExists(dest, connection)) {
                // presumably the user downgraded their XaeroPlus version below 2.15
                // or ran into issues during the migration
                if (tableExists(src, connection)) { // retains existing data in both src and dest
                    statement.executeUpdate("INSERT OR IGNORE INTO \"" + dest + "\" SELECT * FROM \"" + src +"\"");
                    statement.executeUpdate("DROP TABLE IF EXISTS \"" + src + "\"");
                }
            } else {
                statement.executeUpdate("ALTER TABLE \"" + src + "\" RENAME TO \"" + dest + "\"");
            }
        }
    }
}
