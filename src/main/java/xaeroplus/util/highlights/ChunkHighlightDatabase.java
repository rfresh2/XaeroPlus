package xaeroplus.util.highlights;

import com.google.common.collect.Lists;
import xaero.map.WorldMap;
import xaeroplus.XaeroPlus;

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

    public ChunkHighlightDatabase(String worldId, String databaseName) {
        try {
            final Path dbPath = WorldMap.saveFolder.toPath().resolve(worldId).resolve(databaseName + ".db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            createHighlightsTables();
        } catch (Exception e) {
            XaeroPlus.LOGGER.error("Error while creating chunk highlight database", e);
            throw new RuntimeException(e);
        }
    }

    private void createHighlightsTables() {
        try {
            connection.createStatement().execute("CREATE TABLE IF NOT EXISTS \"0\" (x INTEGER, z INTEGER, foundTime INTEGER)");
            connection.createStatement().execute("CREATE TABLE IF NOT EXISTS \"-1\" (x INTEGER, z INTEGER, foundTime INTEGER)");
            connection.createStatement().execute("CREATE TABLE IF NOT EXISTS \"1\" (x INTEGER, z INTEGER, foundTime INTEGER)");
            connection.createStatement().execute("CREATE UNIQUE INDEX IF NOT EXISTS unique_xzO ON \"0\" (x, z)");
            connection.createStatement().execute("CREATE UNIQUE INDEX IF NOT EXISTS unique_xzN ON \"-1\" (x, z)");
            connection.createStatement().execute("CREATE UNIQUE INDEX IF NOT EXISTS unique_xzE ON \"1\" (x, z)");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void insertHighlightList(final List<ChunkHighlightData> chunks, final int dimension) {
        if (chunks.isEmpty()) {
            return;
        }
        if (chunks.size() > MAX_HIGHLIGHTS_LIST) {
            Lists.partition(chunks, MAX_HIGHLIGHTS_LIST).forEach(l -> insertHighlightsListInternal(l, dimension));
        } else {
            insertHighlightsListInternal(chunks, dimension);
        }
    }

    private void insertHighlightsListInternal(final List<ChunkHighlightData> chunks, final int dimension) {
        try {
            String statement = "INSERT OR IGNORE INTO \"" + dimension + "\" VALUES ";
            statement += chunks.stream()
                    .map(chunk -> "(" + chunk.x + ", " + chunk.z + ", " + chunk.foundTime + ")")
                    .collect(Collectors.joining(", "));
            connection.createStatement().execute(statement);
        } catch (Exception e) {
            XaeroPlus.LOGGER.error("Error inserting chunks into database", e);
        }
    }

    public List<ChunkHighlightData> getHighlightsInWindow(
            final int dimension,
            final int regionXMin, final int regionXMax,
            final int regionZMin, final int regionZMax) {
        try {
            ResultSet resultSet = connection.createStatement().executeQuery(
                    "SELECT * FROM \"" + dimension + "\" "
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

    public void removeHighlight(final int x, final int z, final int dimension) {
        try {
            connection.createStatement().execute("DELETE FROM \"" + dimension + "\" WHERE x = " + x + " AND z = " + z);
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
