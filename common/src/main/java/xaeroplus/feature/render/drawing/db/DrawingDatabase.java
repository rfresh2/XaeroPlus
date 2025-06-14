package xaeroplus.feature.render.drawing.db;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongMaps;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.rfresh.sqlite.NativeLibraryNotFoundException;
import org.rfresh.sqlite.SQLiteErrorCode;
import xaero.map.WorldMap;
import xaeroplus.XaeroPlus;
import xaeroplus.feature.render.Line;
import xaeroplus.feature.render.db.DatabaseMigrator;
import xaeroplus.module.ModuleManager;
import xaeroplus.module.impl.TickTaskExecutor;
import xaeroplus.util.ChunkUtils;
import xaeroplus.util.NotificationUtil;

import java.io.Closeable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static xaeroplus.util.ChunkUtils.regionCoordToChunkCoord;

public class DrawingDatabase implements Closeable {
    public static final int MAX_HIGHLIGHTS_LIST = 25000;
    public static final String HIGHLIGHTS_TABLE = "highlights";
    public static final String LINES_TABLE = "lines";
    private Connection connection;
    public final String databaseName;
    protected final Path dbPath;
    private static final DatabaseMigrator MIGRATOR = new DatabaseMigrator(
        List.of(
            new V0Migration()
        )
    );
    boolean recoveryAttempted = false;
    static boolean nativeLibraryErrorSent = false;

    public DrawingDatabase(String worldId, String databaseName) {
        this.databaseName = databaseName;
        try {
            // workaround for other mods that might have forced the JDBC drivers to be init
            // before we are on the classpath
            var jdbcClass = org.rfresh.sqlite.JDBC.class;

            dbPath = WorldMap.saveFolder.toPath().resolve(worldId).resolve(databaseName + ".db");
            boolean init = !dbPath.toFile().exists();
            connection = DriverManager.getConnection("jdbc:rfresh_sqlite:" + dbPath);
            MIGRATOR.migrate(dbPath, databaseName, connection, init);
        } catch (Exception e) {
            if (!nativeLibraryErrorSent && e.getCause() instanceof NativeLibraryNotFoundException nativeException) {
                nativeLibraryErrorSent = true;
                ModuleManager.getModule(TickTaskExecutor.class).execute(() -> {
                    NotificationUtil.errorNotification("Error initializing Drawing database, Drawing features will not work.\n"
                        + nativeException.getMessage());
                });
            }
            XaeroPlus.LOGGER.error("Error while creating drawing database: {} for worldId: {}", databaseName, worldId, e);
            throw new RuntimeException(e);
        }
    }

    private void createHighlightsTable(final String databaseName, final Connection connection, final ResourceKey<Level> dimension) {
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS \"" + getTableName(dimension, HIGHLIGHTS_TABLE) + "\" (x INTEGER, z INTEGER, color INTEGER, PRIMARY KEY (x, z))");
        } catch (SQLException e) {
            XaeroPlus.LOGGER.error("Error creating highlights table for db: {}", databaseName, e);
            throw new RuntimeException(e);
        }
    }

    private void createLinesTable(final String databaseName, final Connection connection, ResourceKey<Level> dimension) {
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS \"" + getTableName(dimension, LINES_TABLE) + "\" (x1 INTEGER, z1 INTEGER, x2 INTEGER, z2 INTEGER, color INTEGER, PRIMARY KEY (x1, z1, x2, z2))");
        } catch (SQLException e) {
            XaeroPlus.LOGGER.error("Error creating lines table for db: {}", databaseName, e);
            throw new RuntimeException(e);
        }
    }

    private String getTableName(ResourceKey<Level> dimension, String type) {
        return dimension.location().toString() + "-" + type;
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

    public void initializeDimension(final ResourceKey<Level> dimension) {
        createHighlightsTable(databaseName, connection, dimension);
        createLinesTable(databaseName, connection, dimension);
    }

    @FunctionalInterface
    public interface LineConsumer {
        void accept(int x1, int z1, int x2, int z2, int color);
    }

    public void getLinesInDimension(
        final ResourceKey<Level> dimension,
        LineConsumer consumer
    ) {
        try (var statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery(
                "SELECT * FROM \"" + getTableName(dimension, LINES_TABLE) + "\"")) {
                while (resultSet.next()) {
                    consumer.accept(
                        resultSet.getInt("x1"),
                        resultSet.getInt("z1"),
                        resultSet.getInt("x2"),
                        resultSet.getInt("z2"),
                        resultSet.getInt("color")
                    );
                }
            }
        } catch (SQLException e) {
            XaeroPlus.LOGGER.error("Error getting lines from {} database in dimension: {}", databaseName, dimension.location(), e);
            if (e.getErrorCode() == SQLiteErrorCode.SQLITE_CORRUPT.code) {
                XaeroPlus.LOGGER.error("Corruption detected in {} database", databaseName, e);
                recoverCorruptDatabase();
            }
        }
    }

    @FunctionalInterface
    public interface HighlightConsumer {
        void accept(int x, int z, int color);
    }

    public void getHighlightsInWindow(
        final ResourceKey<Level> dimension,
        final int regionXMin, final int regionXMax,
        final int regionZMin, final int regionZMax,
        HighlightConsumer consumer
    ) {
        try (var statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery(
                "SELECT * FROM \"" + getTableName(dimension, HIGHLIGHTS_TABLE) + "\" "
                    + "WHERE x >= " + regionCoordToChunkCoord(regionXMin) + " AND x <= " + regionCoordToChunkCoord(regionXMax)
                    + " AND z >= " + regionCoordToChunkCoord(regionZMin) + " AND z <= " + regionCoordToChunkCoord(regionZMax))) {
                while (resultSet.next()) {
                    consumer.accept(
                        resultSet.getInt("x"),
                        resultSet.getInt("z"),
                        resultSet.getInt("color")
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

    public void insertLinesList(final Object2IntMap<Line> lines, final ResourceKey<Level> dimension) {
        if (lines.isEmpty()) return;
        try {
            createLinesTable(databaseName, connection, dimension);
            // Prepared statements is orders of magnitude slower than single insert like this
            // batches even slower
            // only issue is gc spam from string allocations
            int batchSize = MAX_HIGHLIGHTS_LIST;
            StringBuilder sb = new StringBuilder(50 * Math.min(batchSize, lines.size()) + 75);
            var it = Object2IntMaps.fastIterator(lines);
            while (it.hasNext()) {
                sb.setLength(0);
                sb.append("INSERT OR IGNORE INTO \"").append(getTableName(dimension, LINES_TABLE)).append("\" VALUES ");
                boolean trailingComma = false;
                for (int i = 0; i < batchSize && it.hasNext(); i++) {
                    var entry = it.next();
                    var line = entry.getKey();
                    sb.append("(").append(line.x1()).append(", ").append(line.z1()).append(", ").append(line.x2()).append(", ").append(line.z2()).append(", ").append(entry.getIntValue()).append(")");
                    sb.append(", ");
                    trailingComma = true;
                }
                if (trailingComma) sb.replace(sb.length() - 2, sb.length(), "");
                try (var stmt = connection.createStatement()) {
                    stmt.executeUpdate(sb.toString());
                }
            }
        } catch (SQLException e) {
            XaeroPlus.LOGGER.error("Error inserting {} lines into {} database in dimension: {}", lines.size(), databaseName, dimension.location(), e);
            if (e.getErrorCode() == SQLiteErrorCode.SQLITE_CORRUPT.code) {
                XaeroPlus.LOGGER.error("Corruption detected in {} database", databaseName, e);
                recoverCorruptDatabase();
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
                sb.append("INSERT OR IGNORE INTO \"").append(getTableName(dimension, HIGHLIGHTS_TABLE)).append("\" VALUES ");
                boolean trailingComma = false;
                for (int i = 0; i < batchSize && it.hasNext(); i++) {
                    var entry = it.next();
                    var chunk = entry.getLongKey();
                    var chunkX = ChunkUtils.longToChunkX(chunk);
                    var chunkZ = ChunkUtils.longToChunkZ(chunk);
                    var color = entry.getLongValue();
                    sb.append("(").append(chunkX).append(", ").append(chunkZ).append(", ").append(color).append(")");
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

    public void removeLine(final int x1, final int z1, final int x2, final int z2, final ResourceKey<Level> dimension) {
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM \"" + getTableName(dimension, LINES_TABLE) + "\" WHERE x1 = " + x1 + " AND z1 = " + z1 + " AND x2 = " + x2 + " AND z2 = " + z2);
        } catch (SQLException e) {
            XaeroPlus.LOGGER.error("Error while removing line from {} database in dimension: {}, from ({}, {}) to ({}, {})", databaseName, dimension.location(), x1, z1, x2, z2, e);
            if (e.getErrorCode() == SQLiteErrorCode.SQLITE_CORRUPT.code) {
                XaeroPlus.LOGGER.error("Corruption detected in {} database", databaseName, e);
                recoverCorruptDatabase();
            }
        }
    }

    public void removeHighlight(final int x, final int z, final ResourceKey<Level> dimension) {
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM \"" + getTableName(dimension, HIGHLIGHTS_TABLE) + "\" WHERE x = " + x + " AND z = " + z);
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
