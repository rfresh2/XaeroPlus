package xaeroplus.feature.render.highlights;

import com.google.common.collect.Lists;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.rfresh.sqlite.JDBC;
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
import java.util.stream.Collectors;

import static xaeroplus.util.ChunkUtils.regionCoordToChunkCoord;

public class ChunkHighlightDatabase implements Closeable {
    public static int MAX_HIGHLIGHTS_LIST = 25000;
    private final Connection connection;
    protected final String databaseName;
    private static final DatabaseMigrator MIGRATOR = new DatabaseMigrator();

    public ChunkHighlightDatabase(String worldId, String databaseName) {
        this.databaseName = databaseName;
        try {
            // workaround for other mods that might have forced the JDBC drivers to be init
            // before we are on the classpath
            var jdbcClass = JDBC.class;

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
            String statement = "INSERT OR IGNORE INTO \"" + getTableName(dimension) + "\" VALUES ";
            statement += chunks.stream()
                    .map(chunk -> "(" + chunk.x() + ", " + chunk.z() + ", " + chunk.foundTime() + ")")
                    .collect(Collectors.joining(", "));
            try (var stmt = connection.createStatement()) {
                stmt.executeUpdate(statement);
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
                        resultSet.getInt("foundTime")));
                }
                return chunks;
            }
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error getting chunks from {} database in dimension: {}, window: {}-{}, {}-{}", databaseName, dimension.location(), regionXMin, regionXMax, regionZMin, regionZMax, e);
            // fall through
        }
        return Collections.emptyList();
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
