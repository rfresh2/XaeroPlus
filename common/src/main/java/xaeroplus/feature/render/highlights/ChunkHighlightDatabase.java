package xaeroplus.feature.render.highlights;

import com.google.common.collect.Lists;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaero.map.WorldMap;
import xaeroplus.XaeroPlus;
import xaeroplus.feature.render.highlights.db.DatabaseMigrator;

import java.io.Closeable;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static xaeroplus.util.ChunkUtils.regionCoordToChunkCoord;

public class ChunkHighlightDatabase implements Closeable {
    public static final int MAX_HIGHLIGHTS_LIST = 25000;
    private final Connection connection;
    protected final String databaseName;
    private static final DatabaseMigrator MIGRATOR = new DatabaseMigrator();

    public ChunkHighlightDatabase(String worldId, String databaseName) {
        this.databaseName = databaseName;
        try {
            // workaround for other mods that might have forced the JDBC drivers to be init
            // before we are on the classpath
            var jdbcClass = org.rfresh.sqlite.JDBC.class;

            final Path dbPath = WorldMap.saveFolder.toPath().resolve(worldId).resolve(databaseName + ".db");
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
        } catch (Exception e) {
            XaeroPlus.LOGGER.error("Error creating metadata table for db: {}", databaseName);
            throw new RuntimeException(e);
        }
    }

    private void createHighlightsTableIfNotExists(ResourceKey<Level> dimension) {
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS \"" + getTableName(dimension) + "\" (x INTEGER, z INTEGER, foundTime INTEGER)");
            statement.executeUpdate("CREATE UNIQUE INDEX IF NOT EXISTS \"unique_xz_" + getTableName(dimension) + "\" ON \"" + getTableName(dimension) + "\" (x, z)");
        } catch (Exception e) {
            XaeroPlus.LOGGER.error("Error creating highlights table for db: {} in dimension: {}", databaseName, dimension.location());
            throw new RuntimeException(e);
        }
    }

    public void insertHighlightList(final List<ChunkHighlightData> chunks, final ResourceKey<Level> dimension) {
        if (chunks.isEmpty()) {
            return;
        }
        if (chunks.size() > MAX_HIGHLIGHTS_LIST) {
            Lists.partition(chunks, MAX_HIGHLIGHTS_LIST).forEach(l -> insertHighlightsListInternal(l, dimension));
        } else {
            insertHighlightsListInternal(chunks, dimension);
        }
    }

    private void insertHighlightsListInternal(final List<ChunkHighlightData> chunks, final ResourceKey<Level> dimension) {
        try {
            // Prepared statements is orders of magnitude slower than single insert like this
            // batches even slower
            // only issue is gc spam from string allocations
            // could reuse this stringbuilder as this should only be called from a single thread
            StringBuilder sb = new StringBuilder(50 * chunks.size() + 75);
            sb.append("INSERT OR IGNORE INTO \"").append(getTableName(dimension)).append("\" VALUES ");
            for (int i = 0; i < chunks.size(); i++) {
                ChunkHighlightData chunk = chunks.get(i);
                sb.append("(").append(chunk.x()).append(", ").append(chunk.z()).append(", ").append(chunk.foundTime()).append(")");
                if (i < chunks.size() - 1) {
                    sb.append(", ");
                }
            }
            try (var stmt = connection.createStatement()) {
                stmt.executeUpdate(sb.toString());
            }
        } catch (Exception e) {
            XaeroPlus.LOGGER.error("Error inserting {} chunks into {} database in dimension: {}", chunks.size(), databaseName, dimension.location(), e);
        }
    }

    public List<ChunkHighlightData> getHighlightsInWindow(
            final ResourceKey<Level> dimension,
            final int regionXMin, final int regionXMax,
            final int regionZMin, final int regionZMax) {
        try (var statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery(
                "SELECT * FROM \"" + getTableName(dimension) + "\" "
                    + "WHERE x >= " + regionCoordToChunkCoord(regionXMin) + " AND x <= " + regionCoordToChunkCoord(regionXMax)
                    + " AND z >= " + regionCoordToChunkCoord(regionZMin) + " AND z <= " + regionCoordToChunkCoord(regionZMax))) {
                List<ChunkHighlightData> chunks = new ArrayList<>();
                while (resultSet.next()) {
                    chunks.add(new ChunkHighlightData(
                        resultSet.getInt("x"),
                        resultSet.getInt("z"),
                        resultSet.getLong("foundTime")));
                }
                return chunks;
            }
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error getting chunks from {} database in dimension: {}, window: {}-{}, {}-{}", databaseName, dimension.location(), regionXMin, regionXMax, regionZMin, regionZMax, e);
            // fall through
        }
        return Collections.emptyList();
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
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error getting chunks from {} database in dimension: {}, window: {}-{}, {}-{}", databaseName, dimension.location(), regionXMin, regionXMax, regionZMin, regionZMax, e);
            // fall through
        }
    }

    public void removeHighlight(final int x, final int z, final ResourceKey<Level> dimension) {
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM \"" + getTableName(dimension) + "\" WHERE x = " + x + " AND z = " + z);
        } catch (Exception e) {
            XaeroPlus.LOGGER.error("Error while removing highlight from {} database in dimension: {}, at {}, {}", databaseName, dimension.location(), x, z, e);
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
