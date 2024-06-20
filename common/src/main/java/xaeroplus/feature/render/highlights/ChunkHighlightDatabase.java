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
            final Path dbPath = WorldMap.saveFolder.toPath().resolve(worldId).resolve(databaseName + ".db");
            connection = DriverManager.getConnection("jdbc:rfresh_sqlite:" + dbPath);
            MIGRATOR.migrate(dbPath, databaseName, connection);
        } catch (Exception e) {
            XaeroPlus.LOGGER.error("Error while creating chunk highlight database: {}", databaseName, e);
            throw new RuntimeException(e);
        }
    }

    public void initializeDimension(ResourceKey<Level> dimension) {
        createHighlightsTableIfNotExists(dimension);
    }

    private String getTableName(ResourceKey<Level> dimension) {
        return dimension.location().toString();
    }

    private void createHighlightsTableIfNotExists(ResourceKey<Level> dimension) {
        try (var statement = connection.createStatement()) {
            statement.executeQuery("CREATE TABLE IF NOT EXISTS \"" + getTableName(dimension) + "\" (x INTEGER, z INTEGER, foundTime INTEGER)").close();
            statement.executeQuery("CREATE UNIQUE INDEX IF NOT EXISTS \"unique_xz_" + getTableName(dimension) + "\" ON \"" + getTableName(dimension) + "\" (x, z)").close();
        } catch (Exception e) {
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
                stmt.execute(statement);
            }
        } catch (Exception e) {
            XaeroPlus.LOGGER.error("Error inserting chunks into database", e);
        }
    }

    public List<ChunkHighlightData> getHighlightsInWindow(
            final ResourceKey<Level> dimension,
            final int regionXMin, final int regionXMax,
            final int regionZMin, final int regionZMax) {
        try (var statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(
                    "SELECT * FROM \"" + getTableName(dimension) + "\" "
                            + "WHERE x >= " + regionCoordToChunkCoord(regionXMin) + " AND x <= " + regionCoordToChunkCoord(regionXMax)
                            + " AND z >= " + regionCoordToChunkCoord(regionZMin) + " AND z <= " + regionCoordToChunkCoord(regionZMax));
            List<ChunkHighlightData> chunks = new ArrayList<>();
            while (resultSet.next()) {
                chunks.add(new ChunkHighlightData(
                        resultSet.getInt("x"),
                        resultSet.getInt("z"),
                        resultSet.getInt("foundTime")));
            }
            return chunks;
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error getting chunks from database", e);
            // fall through
        }
        return Collections.emptyList();
    }

    public void removeHighlight(final int x, final int z, final ResourceKey<Level> dimension) {
        try (var statement = connection.createStatement()) {
            statement.execute("DELETE FROM \"" + getTableName(dimension) + "\" WHERE x = " + x + " AND z = " + z);
        } catch (Exception e) {
            XaeroPlus.LOGGER.error("Error while removing highlight from database", e);
        }
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (Exception e) {
            XaeroPlus.LOGGER.warn("Failed closing database connection", e);
        }
    }
}
