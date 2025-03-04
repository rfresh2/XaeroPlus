package xaeroplus.feature.render.highlights;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongMaps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.rfresh.sqlite.SQLiteErrorCode;
import xaero.map.WorldMap;
import xaeroplus.XaeroPlus;
import xaeroplus.feature.render.highlights.db.DatabaseMigrator;
import xaeroplus.util.ChunkUtils;

import java.io.Closeable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import static xaeroplus.util.ChunkUtils.regionCoordToChunkCoord;

public class ChunkHighlightDatabase implements Closeable {
    public static final int MAX_HIGHLIGHTS_LIST = 25000;
    private Connection connection;
    protected final String databaseName;
    protected final Path dbPath;
    private static final DatabaseMigrator MIGRATOR = new DatabaseMigrator();
    boolean recoveryAttempted = false;

    public ChunkHighlightDatabase(String worldId, String databaseName) {
        this.databaseName = databaseName;
        try {
            // workaround for other mods that might have forced the JDBC drivers to be init
            // before we are on the classpath
            var jdbcClass = org.rfresh.sqlite.JDBC.class;

            dbPath = WorldMap.saveFolder.toPath().resolve(worldId).resolve(databaseName + ".db");
            boolean shouldRunMigrations = dbPath.toFile().exists();
            connection = DriverManager.getConnection("jdbc:rfresh_sqlite:" + dbPath);
            if (shouldRunMigrations) MIGRATOR.migrate(dbPath, databaseName, connection);
            createMetadataTable();
        } catch (Exception e) {
            XaeroPlus.LOGGER.error("Error while creating chunk highlight database: {} for worldId: {}", databaseName, worldId, e);
            throw new RuntimeException(e);
        }
    }

    public void initializeDimension(ResourceKey<Level> dimension) {
        createHighlightsTableIfNotExists(dimension);
    }

    private String getTableName(ResourceKey<Level> dimension) {
        return dimension.location().toString();
    }

    private void createMetadataTable() {
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS metadata (id INTEGER PRIMARY KEY, version INTEGER)");
            statement.executeUpdate("INSERT OR REPLACE INTO metadata (id, version) VALUES (0, 1)");
        } catch (SQLException e) {
            XaeroPlus.LOGGER.error("Error creating metadata table for db: {}", databaseName, e);
            if (e.getErrorCode() == SQLiteErrorCode.SQLITE_CORRUPT.code) {
                XaeroPlus.LOGGER.error("Corruption detected in {} database", databaseName, e);
                recoverCorruptDatabase();
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    // this can take an extremely long time for large databases
    private void recoverCorruptDatabase() {
        if (recoveryAttempted) {
            // prevent infinite retries if recovery fails
            return;
        }
        recoveryAttempted = true;
        XaeroPlus.LOGGER.info("Attempting to recover corrupt database: {}", databaseName);
        final Path recoveredDbPath = dbPath.getParent().resolve("recovered_" + databaseName + "-" + System.currentTimeMillis() + ".db");
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("recover to \"" + recoveredDbPath.toAbsolutePath() + "\"");
            XaeroPlus.LOGGER.info("Wrote recovered database to: {}", recoveredDbPath);
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error recovering corrupt database: {}", databaseName, e);
            return;
        }
        try {
            connection.close();
            XaeroPlus.LOGGER.info("Closed DB connection to corrupt database: {}", databaseName);
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error closing connection to corrupt database: {}", databaseName, e);
            throw new RuntimeException(e);
        }
        // replace the corrupt database with the recovered one
        // then reopen the connection
        Path corruptedBackDbPath = dbPath.getParent().resolve("corrupted_" + databaseName + "-" + System.currentTimeMillis() + ".db");
        try {
            Files.move(dbPath, corruptedBackDbPath);
            Files.move(recoveredDbPath, dbPath);
            XaeroPlus.LOGGER.info("Replaced corrupt database with recovered: {}", databaseName);
            connection = DriverManager.getConnection("jdbc:rfresh_sqlite:" + dbPath);
            XaeroPlus.LOGGER.info("Opened DB connection to recovered database: {}", databaseName);
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error reopening connection to recovered database: {}", databaseName, e);
            throw new RuntimeException(e);
        }
        try {
            // remove the corrupted backup
            Files.delete(corruptedBackDbPath);
            XaeroPlus.LOGGER.info("Deleted corrupted database backup: {}" , corruptedBackDbPath);
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error deleting corrupted backup database: {}", databaseName, e);
        }
        XaeroPlus.LOGGER.info("Completed recovering corrupt database: {}", databaseName);
    }

    private void createHighlightsTableIfNotExists(ResourceKey<Level> dimension) {
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS \"" + getTableName(dimension) + "\" (x INTEGER, z INTEGER, foundTime INTEGER)");
            statement.executeUpdate("CREATE UNIQUE INDEX IF NOT EXISTS \"unique_xz_" + getTableName(dimension) + "\" ON \"" + getTableName(dimension) + "\" (x, z)");
        } catch (SQLException e) {
            XaeroPlus.LOGGER.error("Error creating highlights table for db: {} in dimension: {}", databaseName, dimension.location(), e);
            if (e.getErrorCode() == SQLiteErrorCode.SQLITE_CORRUPT.code) {
                XaeroPlus.LOGGER.error("Corruption detected in {} database", databaseName, e);
                recoverCorruptDatabase();
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    public void insertHighlightList(final Long2LongMap chunks, final ResourceKey<Level> dimension) {
        if (chunks.isEmpty()) return;
        try {
            // Prepared statements is orders of magnitude slower than single insert like this
            // batches even slower
            // only issue is gc spam from string allocations
            int batchSize = MAX_HIGHLIGHTS_LIST;
            var it = Long2LongMaps.fastIterator(chunks);
            // iterate over entry set, inserting in batches of at most 25000
            StringBuilder sb = new StringBuilder(50 * Math.min(batchSize, chunks.size()) + 75);
            while (it.hasNext()) {
                sb.setLength(0);
                sb.append("INSERT OR IGNORE INTO \"").append(getTableName(dimension)).append("\" VALUES ");
                boolean trailingComma = false;
                for (int i = 0; i < batchSize && it.hasNext(); i++) {
                    var entry = it.next();
                    var chunk = entry.getLongKey();
                    var chunkX = ChunkUtils.longToChunkX(chunk);
                    var chunkZ = ChunkUtils.longToChunkZ(chunk);
                    var foundTime = entry.getLongValue();
                    sb.append("(").append(chunkX).append(", ").append(chunkZ).append(", ").append(foundTime).append(")");
                    sb.append(", ");
                    trailingComma = true;
                }
                if (trailingComma) sb.replace(sb.length() - 2, sb.length(), "");
                try (var stmt = connection.createStatement()) {
                    stmt.executeUpdate(sb.toString());
                }
            }
        } catch (SQLException e) {
            XaeroPlus.LOGGER.error("Error inserting {} chunks into {} database in dimension: {}", chunks.size(), databaseName, dimension.location(), e);
            if (e.getErrorCode() == SQLiteErrorCode.SQLITE_CORRUPT.code) {
                XaeroPlus.LOGGER.error("Corruption detected in {} database", databaseName, e);
                recoverCorruptDatabase();
            }
        }
    }

    @FunctionalInterface
    public interface HighlightConsumer {
        void accept(int x, int z, long foundTime);
    }

    // avoids instantiating the intermediary list
    public void getHighlightsInWindow(
        final ResourceKey<Level> dimension,
        final int regionXMin, final int regionXMax,
        final int regionZMin, final int regionZMax,
        HighlightConsumer consumer
    ) {
        try (var statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery(
                "SELECT * FROM \"" + getTableName(dimension) + "\" "
                    + "WHERE x >= " + regionCoordToChunkCoord(regionXMin) + " AND x <= " + regionCoordToChunkCoord(regionXMax)
                    + " AND z >= " + regionCoordToChunkCoord(regionZMin) + " AND z <= " + regionCoordToChunkCoord(regionZMax))) {
                while (resultSet.next()) {
                    consumer.accept(
                        resultSet.getInt("x"),
                        resultSet.getInt("z"),
                        resultSet.getLong("foundTime")
                    );
                }
            }
        } catch (SQLException e) {
            XaeroPlus.LOGGER.error("Error getting chunks from {} database in dimension: {}, window: {}-{}, {}-{}", databaseName, dimension.location(), regionXMin, regionXMax, regionZMin, regionZMax, e);
            if (e.getErrorCode() == SQLiteErrorCode.SQLITE_CORRUPT.code) {
                XaeroPlus.LOGGER.error("Corruption detected in {} database", databaseName, e);
                recoverCorruptDatabase();
            }
        }
    }

    // avoids instantiating the intermediary list
    public void getHighlightsInWindowAndOutsidePrevWindow(
        final ResourceKey<Level> dimension,
        final int regionXMin, final int regionXMax,
        final int regionZMin, final int regionZMax,
        final int prevRegionXMin, final int prevRegionXMax,
        final int prevRegionZMin, final int prevRegionZMax,
        HighlightConsumer consumer
    ) {
        int xMin = regionCoordToChunkCoord(regionXMin);
        int xMax = regionCoordToChunkCoord(regionXMax);
        int zMin = regionCoordToChunkCoord(regionZMin);
        int zMax = regionCoordToChunkCoord(regionZMax);
        int prevXMin = regionCoordToChunkCoord(prevRegionXMin);
        int prevXMax = regionCoordToChunkCoord(prevRegionXMax);
        int prevZMin = regionCoordToChunkCoord(prevRegionZMin);
        int prevZMax = regionCoordToChunkCoord(prevRegionZMax);
        try (var statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery(
                "SELECT * FROM \"" + getTableName(dimension) + "\" " +
                    "WHERE x BETWEEN " + xMin + " AND " + xMax + " " +
                    "AND z BETWEEN " + zMin + " AND " + zMax + " " +
                    "AND NOT (x BETWEEN " + prevXMin + " AND " + prevXMax + " " +
                    "AND z BETWEEN " + prevZMin + " AND " + prevZMax + ")")) {
                while (resultSet.next()) {
                    consumer.accept(
                        resultSet.getInt("x"),
                        resultSet.getInt("z"),
                        resultSet.getLong("foundTime")
                    );
                }
            }
        } catch (SQLException e) {
            XaeroPlus.LOGGER.error("Error getting chunks from {} database in dimension: {}, window: {}-{}, {}-{}", databaseName, dimension.location(), regionXMin, regionXMax, regionZMin, regionZMax, e);
            if (e.getErrorCode() == SQLiteErrorCode.SQLITE_CORRUPT.code) {
                XaeroPlus.LOGGER.error("Corruption detected in {} database", databaseName, e);
                recoverCorruptDatabase();
            }
            // fall through
        }
    }

    public void removeHighlight(final int x, final int z, final ResourceKey<Level> dimension) {
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM \"" + getTableName(dimension) + "\" WHERE x = " + x + " AND z = " + z);
        } catch (SQLException e) {
            XaeroPlus.LOGGER.error("Error while removing highlight from {} database in dimension: {}, at {}, {}", databaseName, dimension.location(), x, z, e);
            if (e.getErrorCode() == SQLiteErrorCode.SQLITE_CORRUPT.code) {
                XaeroPlus.LOGGER.error("Corruption detected in {} database", databaseName, e);
                recoverCorruptDatabase();
            }
        }
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (Exception e) {
            XaeroPlus.LOGGER.warn("Failed closing {} database connection", databaseName, e);
        }
    }
}
