package xaeroplus.feature.drawing.db;

import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.rfresh.sqlite.NativeLibraryNotFoundException;
import org.rfresh.sqlite.SQLiteConnection;
import org.rfresh.sqlite.SQLiteErrorCode;
import xaero.map.WorldMap;
import xaeroplus.Globals;
import xaeroplus.XaeroPlus;
import xaeroplus.feature.db.DatabaseMigrator;
import xaeroplus.feature.render.ellipse.Ellipse;
import xaeroplus.feature.render.line.Line;
import xaeroplus.feature.render.text.Text;
import xaeroplus.module.impl.TickTaskExecutor;
import xaeroplus.util.ChunkUtils;
import xaeroplus.util.NotificationUtil;
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
import java.util.function.Consumer;

import static xaeroplus.util.ChunkUtils.regionCoordToChunkCoord;
import static xaeroplus.util.ChunkUtils.regionCoordToCoord;

public class DrawingDatabase implements Closeable {
    public static final int MAX_HIGHLIGHTS_LIST = 25000;
    private static final int DATABASE_VERSION = 1;
    public static final String HIGHLIGHTS_TABLE = "highlights";
    public static final String LINES_TABLE = "lines";
    public static final String ELLIPSES_TABLE = "ellipses";
    public static final String TEXTS_TABLE = "texts";
    private Connection connection;
    public final String databaseName;
    protected final Path dbPath;
    private static final DatabaseMigrator MIGRATOR = new DatabaseMigrator(
        List.of(
            new V0Migration(),
            new V1Migration()
        )
    );
    boolean recoveryAttempted = false;
    static boolean nativeLibraryErrorSent = false;
    private static final int MAX_RETRIES = 3;

    public DrawingDatabase(String worldId, String databaseName) {
        this.databaseName = databaseName;
        try {
            // workaround for other mods that might have forced the JDBC drivers to be init
            // before we are on the classpath
            var jdbcClass = org.rfresh.sqlite.JDBC.class;

            dbPath = WorldMap.saveFolder.toPath().resolve(worldId).resolve(databaseName + ".db");
            boolean init = !dbPath.toFile().exists();
            connection = DriverManager.getConnection("jdbc:rfresh_sqlite:" + dbPath);
            ((SQLiteConnection) connection).setBusyTimeout(5000);
            connection = MIGRATOR.migrate(dbPath, databaseName, connection, init);
            validateDbVersion();
            setPragmas();
        } catch (Exception e) {
            if (!nativeLibraryErrorSent && e.getCause() instanceof NativeLibraryNotFoundException nativeException) {
                nativeLibraryErrorSent = true;
                TickTaskExecutor.INSTANCE.execute(() -> {
                    NotificationUtil.errorNotification("Error initializing Drawing database, Drawing features will not work.\n"
                        + nativeException.getMessage());
                });
            }
            XaeroPlus.LOGGER.error("Error while creating drawing database: {} for worldId: {}", databaseName, worldId, e);
            throw new RuntimeException(e);
        }
    }

    int getDatabaseMetadataVersion() {
        var version = DATABASE_VERSION;
        try (var statement = connection.createStatement()) {
            try (var rs = statement.executeQuery("select version from metadata order by time desc limit 1")) {
                rs.next();
                version = rs.getInt("version");
            }
        } catch (Exception e) {
            XaeroPlus.LOGGER.error("Error reading {} database version: {}", databaseName, DATABASE_VERSION, e);
        }
        return version;
    }

    void validateDbVersion() {
        var version = getDatabaseMetadataVersion();
        if (version != DATABASE_VERSION) {
            throw new IllegalStateException(databaseName + " version mismatch: expected " + DATABASE_VERSION + ", found " + version);
        }
    }

    private void setPragmas() {
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("pragma journal_mode = WAL;");
            statement.executeUpdate("pragma synchronous = NORMAL;");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to set sqlite pragmas", e);
        }
    }

    private void createHighlightsTable(final String databaseName, final Connection connection, final ResourceKey<Level> dimension) {
        int tryCount = 0;
        while (tryCount++ < MAX_RETRIES) {
            if (createHighlightsTable0(databaseName, connection, dimension)) {
                return;
            }
            XaeroPlus.LOGGER.info("Retrying creating highlights table for db: {} (attempt {}/{})", databaseName, tryCount, MAX_RETRIES);
            Wait.waitMs(50);
        }
        throw new RuntimeException("Failed to create highlights table for db: " + databaseName);
    }

    private boolean createHighlightsTable0(final String databaseName, final Connection connection, final ResourceKey<Level> dimension) {
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS \"" + getTableName(dimension, HIGHLIGHTS_TABLE) + "\" (x INTEGER, z INTEGER, color INTEGER, PRIMARY KEY (x, z))");
        } catch (SQLException e) {
            XaeroPlus.LOGGER.error("Error creating highlights table for db: {}", databaseName, e);
            return false;
        }
        return true;
    }

    private void createLinesTable(final String databaseName, final Connection connection, ResourceKey<Level> dimension) {
        int tryCount = 0;
        while (tryCount++ < MAX_RETRIES) {
            if (createLinesTable0(databaseName, connection, dimension)) {
                return;
            }
            XaeroPlus.LOGGER.info("Retrying creating lines table for db: {} (attempt {}/{})", databaseName, tryCount, MAX_RETRIES);
            Wait.waitMs(50);
        }
        throw new RuntimeException("Failed to create lines table for db: " + databaseName);
    }

    private boolean createLinesTable0(final String databaseName, final Connection connection, ResourceKey<Level> dimension) {
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS \"" + getTableName(dimension, LINES_TABLE) + "\" (x1 INTEGER, z1 INTEGER, x2 INTEGER, z2 INTEGER, color INTEGER, PRIMARY KEY (x1, z1, x2, z2))");
        } catch (SQLException e) {
            XaeroPlus.LOGGER.error("Error creating lines table for db: {}", databaseName, e);
            return false;
        }
        return true;
    }

    private void createEllipsesTable(final String databaseName, final Connection connection, final ResourceKey<Level> dimension) {
        var tryCount = 0;
        while (tryCount++ < MAX_RETRIES) {
            if (createEllipsesTable0(databaseName, connection, dimension)) return;
            XaeroPlus.LOGGER.info("Retrying creating ellipses table for db: {} (attempt {}/{})", databaseName, tryCount, MAX_RETRIES);
            Wait.waitMs(50);
        }
        throw new RuntimeException("Failed to create ellipses table for db: " + databaseName);
    }

    private boolean createEllipsesTable0(final String databaseName, final Connection connection, final ResourceKey<Level> dimension) {
        try (var statement = connection.createStatement()) {
            statement.executeUpdate(
                "CREATE TABLE IF NOT EXISTS \"" + getTableName(dimension, ELLIPSES_TABLE)
                    + "\" (centerX INTEGER, centerZ INTEGER, radiusX INTEGER, radiusZ INTEGER, color INTEGER,"
                    + " PRIMARY KEY (centerX, centerZ, radiusX, radiusZ))"
            );
        } catch (final SQLException e) {
            XaeroPlus.LOGGER.error("Error creating ellipses table for db: {}", databaseName, e);
            return false;
        }
        return true;
    }

    private void createTextsTable(final String databaseName, final Connection connection, ResourceKey<Level> dimension) {
        int tryCount = 0;
        while (tryCount++ < MAX_RETRIES) {
            if (createTextsTable0(databaseName, connection, dimension)) {
                return;
            }
            XaeroPlus.LOGGER.info("Retrying creating texts table for db: {} (attempt {}/{})", databaseName, tryCount, MAX_RETRIES);
            Wait.waitMs(50);
        }
        throw new RuntimeException("Failed to create texts table for db: " + databaseName);
    }

    private boolean createTextsTable0(final String databaseName, final Connection connection, ResourceKey<Level> dimension) {
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS \"" + getTableName(dimension, TEXTS_TABLE) + "\" (value TEXT, x INTEGER, z INTEGER, color INTEGER, scale REAL, PRIMARY KEY (x, z))");
        } catch (SQLException e) {
            XaeroPlus.LOGGER.error("Error creating texts table for db: {}", databaseName, e);
            return false;
        }
        return true;
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

    public void initializeDimension(final ResourceKey<Level> dimension) {
        createHighlightsTable(databaseName, connection, dimension);
        createLinesTable(databaseName, connection, dimension);
        createEllipsesTable(databaseName, connection, dimension);
        createTextsTable(databaseName, connection, dimension);
    }

    @FunctionalInterface
    public interface LineConsumer {
        void accept(int x1, int z1, int x2, int z2, int color);
    }

    public void getLinesInDimension(
        final ResourceKey<Level> dimension,
        LineConsumer consumer
    ) {
        int tryCount = 0;
        while (tryCount++ < MAX_RETRIES) {
            if (getLinesInDimension0(dimension, consumer)) {
                return;
            }
            XaeroPlus.LOGGER.info("Retrying getting lines from {} database in dimension: {} (attempt {}/{})", databaseName, dimension.location(), tryCount, MAX_RETRIES);
            Wait.waitMs(50);
        }
    }

    private boolean getLinesInDimension0(
        final ResourceKey<Level> dimension,
        LineConsumer consumer
    ) {
        try (var statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery(
                "SELECT x1, z1, x2, z2, color FROM \"" + getTableName(dimension, LINES_TABLE) + "\"")) {
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
            return false;
        }
        return true;
    }

    @FunctionalInterface
    public interface EllipseConsumer {
        void accept(int centerX, int centerZ, int radiusX, int radiusZ, int color);
    }

    public void getEllipsesInDimension(final ResourceKey<Level> dimension, final EllipseConsumer consumer) {
        var tryCount = 0;
        while (tryCount++ < MAX_RETRIES) {
            if (getEllipsesInDimension0(dimension, consumer)) return;
            XaeroPlus.LOGGER.info("Retrying getting ellipses from {} database in dimension: {} (attempt {}/{})", databaseName, dimension.location(), tryCount, MAX_RETRIES);
            Wait.waitMs(50);
        }
    }

    private boolean getEllipsesInDimension0(final ResourceKey<Level> dimension, final EllipseConsumer consumer) {
        try (var statement = connection.createStatement();
             var resultSet = statement.executeQuery("SELECT centerX, centerZ, radiusX, radiusZ, color FROM \"" + getTableName(dimension, ELLIPSES_TABLE) + "\"")) {
            while (resultSet.next()) {
                consumer.accept(
                    resultSet.getInt("centerX"),
                    resultSet.getInt("centerZ"),
                    resultSet.getInt("radiusX"),
                    resultSet.getInt("radiusZ"),
                    resultSet.getInt("color")
                );
            }
        } catch (final SQLException e) {
            XaeroPlus.LOGGER.error("Error getting ellipses from {} database in dimension: {}", databaseName, dimension.location(), e);
            if (e.getErrorCode() == SQLiteErrorCode.SQLITE_CORRUPT.code) {
                recoverCorruptDatabase();
            }
            return false;
        }
        return true;
    }

    public void getTextsInWindow(
        final ResourceKey<Level> dimension,
        final int regionXMin, final int regionXMax,
        final int regionZMin, final int regionZMax,
        Consumer<Text> consumer
    ) {
        int tryCount = 0;
        while (tryCount++ < MAX_RETRIES) {
            if (getTextsInWindow0(dimension, regionXMin, regionXMax, regionZMin, regionZMax, consumer)) {
                return;
            }
            XaeroPlus.LOGGER.info("Retrying getting texts from {} database in dimension: {} (attempt {}/{})", databaseName, dimension.location(), tryCount, MAX_RETRIES);
            Wait.waitMs(50);
        }
    }

    private boolean getTextsInWindow0(
        final ResourceKey<Level> dimension,
        final int regionXMin, final int regionXMax,
        final int regionZMin, final int regionZMax,
        Consumer<Text> consumer
    ) {
        try (var statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery(
                "SELECT value, x, z, color, scale FROM \"" + getTableName(dimension, TEXTS_TABLE) + "\" "
                    + "WHERE x >= " + regionCoordToCoord(regionXMin) + " AND x <= " + regionCoordToCoord(regionXMax)
                    + " AND z >= " + regionCoordToCoord(regionZMin) + " AND z <= " + regionCoordToCoord(regionZMax))) {
                while (resultSet.next()) {
                    var text = new Text(
                        resultSet.getString("value"),
                        resultSet.getInt("x"),
                        resultSet.getInt("z"),
                        resultSet.getInt("color"),
                        resultSet.getFloat("scale")
                    );
                    consumer.accept(text);
                }
            }
        } catch (SQLException e) {
            XaeroPlus.LOGGER.error("Error getting texts from {} database in dimension: {}", databaseName, dimension.location(), e);
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
        void accept(int x, int z, int color);
    }

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
            XaeroPlus.LOGGER.info("Retrying getting highlights from {} database in dimension: {} (attempt {}/{})", databaseName, dimension.location(), tryCount, MAX_RETRIES);
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
                "SELECT x, z, color FROM \"" + getTableName(dimension, HIGHLIGHTS_TABLE) + "\" "
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
            return false;
        }
        return true;
    }

    public void insertLinesList(final Object2IntMap<Line> lines, final ResourceKey<Level> dimension) {
        int tryCount = 0;
        while (tryCount++ < MAX_RETRIES) {
            if (insertLinesList0(lines, dimension)) {
                return;
            }
            XaeroPlus.LOGGER.info("Retrying inserting {} lines into {} database in dimension: {} (attempt {}/{})", lines.size(), databaseName, dimension.location(), tryCount, MAX_RETRIES);
            Wait.waitMs(50);
        }
    }

    private boolean insertLinesList0(final Object2IntMap<Line> lines, final ResourceKey<Level> dimension) {
        if (lines.isEmpty()) return true;
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
                sb.append("INSERT OR REPLACE INTO \"").append(getTableName(dimension, LINES_TABLE)).append("\" (x1, z1, x2, z2, color) VALUES ");
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
            return false;
        }
        return true;
    }

    public void insertEllipsesList(final Object2IntMap<Ellipse> ellipses, final ResourceKey<Level> dimension) {
        var tryCount = 0;
        while (tryCount++ < MAX_RETRIES) {
            if (insertEllipsesList0(ellipses, dimension)) return;
            XaeroPlus.LOGGER.info("Retrying inserting {} ellipses into {} database in dimension: {} (attempt {}/{})", ellipses.size(), databaseName, dimension.location(), tryCount, MAX_RETRIES);
            Wait.waitMs(50);
        }
    }

    private boolean insertEllipsesList0(final Object2IntMap<Ellipse> ellipses, final ResourceKey<Level> dimension) {
        if (ellipses.isEmpty()) return true;
        try {
            createEllipsesTable(databaseName, connection, dimension);
            var batchSize = MAX_HIGHLIGHTS_LIST;
            var sql = new StringBuilder(60 * Math.min(batchSize, ellipses.size()) + 75);
            var iterator = Object2IntMaps.fastIterator(ellipses);
            while (iterator.hasNext()) {
                sql.setLength(0);
                sql.append("INSERT OR REPLACE INTO \"").append(getTableName(dimension, ELLIPSES_TABLE)).append("\" (centerX, centerZ, radiusX, radiusZ, color) VALUES ");
                for (var i = 0; i < batchSize && iterator.hasNext(); i++) {
                    var entry = iterator.next();
                    var ellipse = entry.getKey();
                    sql.append("(")
                        .append(ellipse.centerX()).append(", ")
                        .append(ellipse.centerZ()).append(", ")
                        .append(ellipse.radiusX()).append(", ")
                        .append(ellipse.radiusZ()).append(", ")
                        .append(entry.getIntValue()).append("), ");
                }
                sql.setLength(sql.length() - 2);
                try (var statement = connection.createStatement()) {
                    statement.executeUpdate(sql.toString());
                }
            }
        } catch (final SQLException e) {
            XaeroPlus.LOGGER.error("Error inserting {} ellipses into {} database in dimension: {}", ellipses.size(), databaseName, dimension.location(), e);
            if (e.getErrorCode() == SQLiteErrorCode.SQLITE_CORRUPT.code) {
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
            XaeroPlus.LOGGER.info("Retrying inserting {} chunks into {} database in dimension: {} (attempt {}/{})", chunks.size(), databaseName, dimension.location(), tryCount, MAX_RETRIES);
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
                sb.append("INSERT OR REPLACE INTO \"").append(getTableName(dimension, HIGHLIGHTS_TABLE)).append("\" (x, z, color) VALUES ");
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
            return false;
        }
        return true;
    }

    public void insertTextsList(final Long2ObjectMap<Text> texts, final ResourceKey<Level> dimension) {
        int tryCount = 0;
        while (tryCount++ < MAX_RETRIES) {
            if (insertTextsList0(texts, dimension)) {
                return;
            }
            XaeroPlus.LOGGER.info("Retrying inserting {} texts into {} database in dimension: {} (attempt {}/{})", texts.size(), databaseName, dimension.location(), tryCount, MAX_RETRIES);
            Wait.waitMs(50);
        }
    }

    private boolean insertTextsList0(final Long2ObjectMap<Text> texts, final ResourceKey<Level> dimension) {
        if (texts.isEmpty()) return true;
        try {
            // Prepared statements is orders of magnitude slower than single insert like this
            // batches even slower
            // only issue is gc spam from string allocations
            int batchSize = MAX_HIGHLIGHTS_LIST;
            var it = Long2ObjectMaps.fastIterator(texts);
            // iterate over entry set, inserting in batches of at most 25000
            StringBuilder sb = new StringBuilder(50 * Math.min(batchSize, texts.size()) + 75);
            while (it.hasNext()) {
                sb.setLength(0);
                sb.append("INSERT OR REPLACE INTO \"").append(getTableName(dimension, TEXTS_TABLE)).append("\" (value, x, z, color, scale) VALUES ");
                boolean trailingComma = false;
                for (int i = 0; i < batchSize && it.hasNext(); i++) {
                    var entry = it.next().getValue();
                    sb
                        .append("(")
                        .append("'").append(entry.value()).append("', ")
                        .append(entry.x()).append(", ")
                        .append(entry.z()).append(", ")
                        .append(entry.color()).append(", ")
                        .append(entry.scale())
                        .append(")");
                    sb.append(", ");
                    trailingComma = true;
                }
                if (trailingComma) sb.replace(sb.length() - 2, sb.length(), "");
                try (var stmt = connection.createStatement()) {
                    stmt.executeUpdate(sb.toString());
                }
            }
        } catch (SQLException e) {
            XaeroPlus.LOGGER.error("Error inserting {} texts into {} database in dimension: {}", texts.size(), databaseName, dimension.location(), e);
            if (e.getErrorCode() == SQLiteErrorCode.SQLITE_CORRUPT.code) {
                XaeroPlus.LOGGER.error("Corruption detected in {} database", databaseName, e);
                recoverCorruptDatabase();
            }
            return false;
        }
        return true;
    }

    public void removeLine(final int x1, final int z1, final int x2, final int z2, final ResourceKey<Level> dimension) {
        int tryCount = 0;
        while (tryCount++ < MAX_RETRIES) {
            if (removeLine0(x1, z1, x2, z2, dimension)) {
                return;
            }
            XaeroPlus.LOGGER.info("Retrying removing line from {} database in dimension: {}, from ({}, {}) to ({}, {}) (attempt {}/{})", databaseName, dimension.location(), x1, z1, x2, z2, tryCount, MAX_RETRIES);
            Wait.waitMs(50);
        }
    }

    private boolean removeLine0(final int x1, final int z1, final int x2, final int z2, final ResourceKey<Level> dimension) {
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM \"" + getTableName(dimension, LINES_TABLE) + "\" WHERE x1 = " + x1 + " AND z1 = " + z1 + " AND x2 = " + x2 + " AND z2 = " + z2);
        } catch (SQLException e) {
            XaeroPlus.LOGGER.error("Error while removing line from {} database in dimension: {}", databaseName, dimension.location(), e);
            if (e.getErrorCode() == SQLiteErrorCode.SQLITE_CORRUPT.code) {
                XaeroPlus.LOGGER.error("Corruption detected in {} database", databaseName, e);
                recoverCorruptDatabase();
            }
            return false;
        }
        return true;
    }

    public void removeAllLines(final ResourceKey<Level> dimension) {
        int tryCount = 0;
        while (tryCount++ < MAX_RETRIES) {
            if (removeAllLines0(dimension)) {
                return;
            }
            XaeroPlus.LOGGER.info("Retrying removing all lines from {} database in dimension: {}, (attempt {}/{})", databaseName, dimension.location(), tryCount, MAX_RETRIES);
            Wait.waitMs(50);
        }
    }

    private boolean removeAllLines0(final ResourceKey<Level> dimension) {
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM \"" + getTableName(dimension, LINES_TABLE) + "\"");
        } catch (SQLException e) {
            XaeroPlus.LOGGER.error("Error while removing all lines from {} database in dimension: {}", databaseName, dimension.location(), e);
            if (e.getErrorCode() == SQLiteErrorCode.SQLITE_CORRUPT.code) {
                XaeroPlus.LOGGER.error("Corruption detected in {} database", databaseName, e);
                recoverCorruptDatabase();
            }
            return false;
        }
        return true;
    }

    public void removeEllipse(final Ellipse ellipse, final ResourceKey<Level> dimension) {
        var tryCount = 0;
        while (tryCount++ < MAX_RETRIES) {
            if (removeEllipse0(ellipse, dimension)) return;
            XaeroPlus.LOGGER.info("Retrying removing ellipse from {} database in dimension: {} (attempt {}/{})", databaseName, dimension.location(), tryCount, MAX_RETRIES);
            Wait.waitMs(50);
        }
    }

    private boolean removeEllipse0(final Ellipse ellipse, final ResourceKey<Level> dimension) {
        try (var statement = connection.createStatement()) {
            statement.executeUpdate(
                "DELETE FROM \"" + getTableName(dimension, ELLIPSES_TABLE) + "\" WHERE centerX = " + ellipse.centerX()
                    + " AND centerZ = " + ellipse.centerZ()
                    + " AND radiusX = " + ellipse.radiusX()
                    + " AND radiusZ = " + ellipse.radiusZ()
            );
        } catch (final SQLException e) {
            XaeroPlus.LOGGER.error("Error removing ellipse from {} database in dimension: {}", databaseName, dimension.location(), e);
            if (e.getErrorCode() == SQLiteErrorCode.SQLITE_CORRUPT.code) {
                recoverCorruptDatabase();
            }
            return false;
        }
        return true;
    }

    public void removeAllEllipses(final ResourceKey<Level> dimension) {
        var tryCount = 0;
        while (tryCount++ < MAX_RETRIES) {
            if (removeAllEllipses0(dimension)) return;
            XaeroPlus.LOGGER.info("Retrying removing all ellipses from {} database in dimension: {} (attempt {}/{})", databaseName, dimension.location(), tryCount, MAX_RETRIES);
            Wait.waitMs(50);
        }
    }

    private boolean removeAllEllipses0(final ResourceKey<Level> dimension) {
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM \"" + getTableName(dimension, ELLIPSES_TABLE) + "\"");
        } catch (final SQLException e) {
            XaeroPlus.LOGGER.error("Error removing all ellipses from {} database in dimension: {}", databaseName, dimension.location(), e);
            if (e.getErrorCode() == SQLiteErrorCode.SQLITE_CORRUPT.code) {
                recoverCorruptDatabase();
            }
            return false;
        }
        return true;
    }

    public void removeHighlight(final int x, final int z, final ResourceKey<Level> dimension) {
        int tryCount = 0;
        while (tryCount++ < MAX_RETRIES) {
            if (removeHighlight0(x, z, dimension)) {
                return;
            }
            XaeroPlus.LOGGER.info("Retrying removing highlight from {} database in dimension: {} (attempt {}/{})", databaseName, dimension.location(), tryCount, MAX_RETRIES);
            Wait.waitMs(50);
        }
    }

    private boolean removeHighlight0(final int x, final int z, final ResourceKey<Level> dimension) {
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM \"" + getTableName(dimension, HIGHLIGHTS_TABLE) + "\" WHERE x = " + x + " AND z = " + z);
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

    public void removeHighlights(final LongCollection toRemove, final ResourceKey<Level> dimension) {
        int tryCount = 0;
        while (tryCount++ < 3) {
            if (removeHighlights0(toRemove, dimension)) {
                return;
            }
            XaeroPlus.LOGGER.info("Retrying removal of highlight set from {} database in dimension: {} (attempt {}/{})", databaseName, dimension.location(), tryCount, 3);
            Wait.waitMs(50);
        }
    }

    private boolean removeHighlights0(final LongCollection toRemove, final ResourceKey<Level> dimension) {
        if (toRemove.isEmpty()) return true;
        try {
            int batchSize = 500;
            var it = toRemove.longIterator();
            var sb = new StringBuilder(50 * Math.min(batchSize, toRemove.size()) + 75);
            while (it.hasNext()) {
                sb.setLength(0);
                sb.append("DELETE FROM \"").append(getTableName(dimension, HIGHLIGHTS_TABLE)).append("\" WHERE ");
                for (int i = 0; i < batchSize && it.hasNext(); i++) {
                    var chunk = it.nextLong();
                    var chunkX = ChunkUtils.longToChunkX(chunk);
                    var chunkZ = ChunkUtils.longToChunkZ(chunk);
                    sb.append("(x=").append(chunkX).append(" AND z=").append(chunkZ).append(")");
                    if (i < batchSize - 1 && it.hasNext()) sb.append(" OR ");
                }
                try (var statement = connection.createStatement()) {
                    statement.executeUpdate(sb.toString());
                }
            }
        } catch (SQLException e) {
            XaeroPlus.LOGGER.error("Error while removing highlight set from {} database in dimension: {} with {} highlights", databaseName, dimension.location(), toRemove.size(), e);
            if (e.getErrorCode() == SQLiteErrorCode.SQLITE_CORRUPT.code) {
                XaeroPlus.LOGGER.error("Corruption detected in {} database", databaseName, e);
                recoverCorruptDatabase();
            }
            return false;
        }
        return true;
    }

    public void removeAllHighlights(final ResourceKey<Level> dimension) {
        int tryCount = 0;
        while (tryCount++ < 3) {
            if (removeAllHighlights0(dimension)) {
                return;
            }
            XaeroPlus.LOGGER.info("Retrying removing all highlights from {} database in dimension: {} (attempt {}/{})", databaseName, dimension.location(), tryCount, 3);
            Wait.waitMs(50);
        }
    }

    private boolean removeAllHighlights0(final ResourceKey<Level> dimension) {
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM \"" + getTableName(dimension, HIGHLIGHTS_TABLE) + "\"");
        } catch (SQLException e) {
            XaeroPlus.LOGGER.error("Error while removing all highlights from {} database in dimension: {}", databaseName, dimension.location(), e);
            if (e.getErrorCode() == SQLiteErrorCode.SQLITE_CORRUPT.code) {
                XaeroPlus.LOGGER.error("Corruption detected in {} database", databaseName, e);
                recoverCorruptDatabase();
            }
            return false;
        }
        return true;
    }

    public void removeText(final int x, final int z, final ResourceKey<Level> dimension) {
        int tryCount = 0;
        while (tryCount++ < MAX_RETRIES) {
            if (removeText0(x, z, dimension)) {
                return;
            }
            XaeroPlus.LOGGER.info("Retrying removing text from {} database in dimension: {}, at {}, {} (attempt {}/{})", databaseName, dimension.location(), x, z, tryCount, MAX_RETRIES);
            Wait.waitMs(50);
        }
    }

    private boolean removeText0(final int x, final int z, final ResourceKey<Level> dimension) {
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM \"" + getTableName(dimension, TEXTS_TABLE) + "\" WHERE x = " + x + " AND z = " + z);
        } catch (SQLException e) {
            XaeroPlus.LOGGER.error("Error while removing text from {} database in dimension: {}, at {}, {}", databaseName, dimension.location(), x, z, e);
            if (e.getErrorCode() == SQLiteErrorCode.SQLITE_CORRUPT.code) {
                XaeroPlus.LOGGER.error("Corruption detected in {} database", databaseName, e);
                recoverCorruptDatabase();
            }
            return false;
        }
        return true;
    }

    public void removeAllTexts(final ResourceKey<Level> dimension) {
        int tryCount = 0;
        while (tryCount++ < MAX_RETRIES) {
            if (removeAllTexts0(dimension)) {
                return;
            }
            XaeroPlus.LOGGER.info("Retrying removing all texts from {} database in dimension: {}, (attempt {}/{})", databaseName, dimension.location(), tryCount, MAX_RETRIES);
            Wait.waitMs(50);
        }
    }

    private boolean removeAllTexts0(final ResourceKey<Level> dimension) {
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM \"" + getTableName(dimension, TEXTS_TABLE) + "\"");
        } catch (SQLException e) {
            XaeroPlus.LOGGER.error("Error while removing all texts from {} database in dimension: {}", databaseName, dimension.location(), e);
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
