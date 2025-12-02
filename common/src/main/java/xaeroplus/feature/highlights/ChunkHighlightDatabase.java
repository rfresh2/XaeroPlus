package xaeroplus.feature.highlights;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongMaps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.rfresh.sqlite.SQLiteConnection;
import org.rfresh.sqlite.SQLiteErrorCode;
import xaero.map.WorldMap;
import xaeroplus.Globals;
import xaeroplus.XaeroPlus;
import xaeroplus.feature.db.DatabaseMigrator;
import xaeroplus.feature.highlights.db.V0ToV1Migration;
import xaeroplus.util.ChunkUtils;
import xaeroplus.util.Wait;

import java.io.Closeable;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static xaeroplus.util.ChunkUtils.regionCoordToChunkCoord;

public class ChunkHighlightDatabase implements Closeable {
    public static final int MAX_HIGHLIGHTS_LIST = 25000;
    private Connection connection;
    protected final String databaseName;
    protected final Path dbPath;
    private static final DatabaseMigrator MIGRATOR = new DatabaseMigrator(
        List.of(
            new V0ToV1Migration()
        )
    );
    boolean recoveryAttempted = false;
    private static final int MAX_RETRIES = 3;

    public ChunkHighlightDatabase(String worldId, String databaseName) {
        this.databaseName = databaseName;
        try {
            // workaround for other mods that might have forced the JDBC drivers to be init
            // before we are on the classpath
            var jdbcClass = org.rfresh.sqlite.JDBC.class;

            dbPath = WorldMap.saveFolder.toPath().resolve(worldId).resolve(databaseName + ".db");
            boolean init = !dbPath.toFile().exists();
            connection = DriverManager.getConnection("jdbc:rfresh_sqlite:" + dbPath);
            ((SQLiteConnection) connection).setBusyTimeout(5000);
            MIGRATOR.migrate(dbPath, databaseName, connection, init);
            createMetadataTable();
            setPragmas();
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

    private void setPragmas() {
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("pragma journal_mode = WAL;");
            statement.executeUpdate("pragma synchronous = NORMAL;");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to set sqlite pragmas", e);
        }
    }

    private void createMetadataTable() {
        int tryCount = 0;
        while (tryCount++ < MAX_RETRIES) {
            if (createMetadataTable0()) {
                return;
            }
            XaeroPlus.LOGGER.info("Retrying creation of metadata table in {} database (attempt {}/{})", databaseName, tryCount, 3);
            Wait.waitMs(50);
        }
        throw new RuntimeException("Failed to create metadata table");
    }

    private boolean createMetadataTable0() {
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS metadata (id INTEGER PRIMARY KEY, version INTEGER)");
            statement.executeUpdate("INSERT OR REPLACE INTO metadata (id, version) VALUES (0, 1)");
        } catch (SQLException e) {
            XaeroPlus.LOGGER.error("Error creating metadata table for db: {}", databaseName, e);
            if (e.getErrorCode() == SQLiteErrorCode.SQLITE_CORRUPT.code) {
                XaeroPlus.LOGGER.error("Corruption detected in {} database", databaseName, e);
                recoverCorruptDatabase();
            }
            return false;
        }
        return true;
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
        Path originalJournalDbPath = dbPath.getParent().resolve(dbPath.getFileName() + "-journal");
        Path recoveredJournalDbPath = recoveredDbPath.getParent().resolve(recoveredDbPath.getFileName() + "-journal");
        // replace the corrupt database with the recovered one
        // then reopen the connection
        Path corruptedBackDbPath = dbPath.getParent().resolve("corrupted_" + databaseName + "-" + System.currentTimeMillis() + ".db");
        Path corruptedBackJournalDbPath = corruptedBackDbPath.getParent().resolve(corruptedBackDbPath.getFileName() + "-journal");
        CopyOption[] copyOptions;
        if (Globals.atomicMoveAvailable) {
            copyOptions = new CopyOption[]{StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE};
        } else {
            copyOptions = new CopyOption[]{StandardCopyOption.REPLACE_EXISTING};
        }
        try {
            Files.move(dbPath, corruptedBackDbPath, copyOptions);
            if (originalJournalDbPath.toFile().exists()) {
                Files.move(originalJournalDbPath, corruptedBackJournalDbPath, copyOptions);
            }
            Files.move(recoveredDbPath, dbPath, copyOptions);
            if (recoveredJournalDbPath.toFile().exists()) {
                Files.move(recoveredJournalDbPath, originalJournalDbPath, copyOptions);
            }
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
            if (corruptedBackJournalDbPath.toFile().exists()) {
                Files.delete(corruptedBackJournalDbPath);
            }
            XaeroPlus.LOGGER.info("Deleted corrupted database backup: {}" , corruptedBackDbPath);
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error deleting corrupted backup database: {}", databaseName, e);
        }
        XaeroPlus.LOGGER.info("Completed recovering corrupt database: {}", databaseName);
    }

    private void createHighlightsTableIfNotExists(ResourceKey<Level> dimension) {
        int tryCount = 0;
        while (tryCount++ < MAX_RETRIES) {
            if (createHighlightsTableIfNotExists0(dimension)) {
                return;
            }
            XaeroPlus.LOGGER.info("Retrying creation of highlights table in {} database in dimension: {} (attempt {}/{})", databaseName, dimension.location(), tryCount, 3);
            Wait.waitMs(50);
        }
    }

    private boolean createHighlightsTableIfNotExists0(ResourceKey<Level> dimension) {
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS \"" + getTableName(dimension) + "\" (x INTEGER, z INTEGER, foundTime INTEGER)");
            statement.executeUpdate("CREATE UNIQUE INDEX IF NOT EXISTS \"unique_xz_" + getTableName(dimension) + "\" ON \"" + getTableName(dimension) + "\" (x, z)");
        } catch (SQLException e) {
            XaeroPlus.LOGGER.error("Error creating highlights table for db: {} in dimension: {}", databaseName, dimension.location(), e);
            if (e.getErrorCode() == SQLiteErrorCode.SQLITE_CORRUPT.code) {
                XaeroPlus.LOGGER.error("Corruption detected in {} database", databaseName, e);
                recoverCorruptDatabase();
            }
            return false;
        }
        return true;
    }

    public void insertHighlightList(final Long2LongMap chunks, final ResourceKey<Level> dimension) {
        int tryCount = 0;
        while (tryCount++ < MAX_RETRIES) {
            if (insertHighlightList0(chunks, dimension)) {
                return;
            }
            XaeroPlus.LOGGER.info("Retrying insert of {} chunks into {} database in dimension: {} (attempt {}/{})", chunks.size(), databaseName, dimension.location(), tryCount, 3);
            Wait.waitMs(50);
        }
    }

    private boolean insertHighlightList0(final Long2LongMap chunks, final ResourceKey<Level> dimension) {
        if (chunks.isEmpty()) return true;
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
            return false;
        }
        return true;
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
        int tryCount = 0;
        while (tryCount++ < MAX_RETRIES) {
            if (getHighlightsInWindow0(dimension, regionXMin, regionXMax, regionZMin, regionZMax, consumer)) {
                return;
            }
            XaeroPlus.LOGGER.info("Retrying get highlights from {} database in dimension: {}, (attempt {}/{})", databaseName, dimension.location(), tryCount, 3);
            Wait.waitMs(50);
        }
    }

    private boolean getHighlightsInWindow0(
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
            return false;
        }
        return true;
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
        int tryCount = 0;
        while (tryCount++ < MAX_RETRIES) {
            if (getHighlightsInWindowAndOutsidePrevWindow0(dimension, regionXMin, regionXMax, regionZMin, regionZMax, prevRegionXMin, prevRegionXMax, prevRegionZMin, prevRegionZMax, consumer)) {
                return;
            }
            XaeroPlus.LOGGER.info("Retrying get of highlights from {} database in dimension: {}, (attempt {}/{})", databaseName, dimension.location(), tryCount, 3);
            Wait.waitMs(50);
        }
    }

    private boolean getHighlightsInWindowAndOutsidePrevWindow0(
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
            return false;
        }
        return true;
    }

    public void removeHighlight(final int x, final int z, final ResourceKey<Level> dimension) {
        int tryCount = 0;
        while (tryCount++ < 3) {
            if (removeHighlight0(x, z, dimension)) {
                return;
            }
            XaeroPlus.LOGGER.info("Retrying removal of highlight from {} database in dimension: {} (attempt {}/{})", databaseName, dimension.location(), tryCount, 3);
            Wait.waitMs(50);
        }
    }

    private boolean removeHighlight0(final int x, final int z, final ResourceKey<Level> dimension) {
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM \"" + getTableName(dimension) + "\" WHERE x = " + x + " AND z = " + z);
        } catch (SQLException e) {
            XaeroPlus.LOGGER.error("Error while removing highlight from {} database in dimension: {}, at {}, {}", databaseName, dimension.location(), x, z, e);
            if (e.getErrorCode() == SQLiteErrorCode.SQLITE_CORRUPT.code) {
                XaeroPlus.LOGGER.error("Corruption detected in {} database", databaseName, e);
                recoverCorruptDatabase();
            }
            return false;
        }
        return true;
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
