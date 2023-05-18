package xaeroplus.util.newchunks;

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

public class NewChunksDatabase implements Closeable {
    public static final int MAX_NEWCHUNKS_LIST_SIZE = 25000;
    private final Connection connection;
    public NewChunksDatabase(String worldId) {
        try {
            final Path dbPath = WorldMap.saveFolder.toPath().resolve(worldId).resolve("XaeroPlusNewChunks.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            createNewChunksTable();
        } catch (Exception e) {
            XaeroPlus.LOGGER.error("Error while creating new chunks database", e);
            throw new RuntimeException(e);
        }
    }

    public void createNewChunksTable() {
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

    public void insertNewChunkList(final List<NewChunkData> newChunks, final int dimension) {
        if (newChunks.isEmpty()) {
            return;
        }
        if (newChunks.size() > MAX_NEWCHUNKS_LIST_SIZE) {
            Lists.partition(newChunks, MAX_NEWCHUNKS_LIST_SIZE).forEach(l -> insertNewChunksListInternal(l, dimension));
        } else {
            insertNewChunksListInternal(newChunks, dimension);
        }
    }

    private void insertNewChunksListInternal(final List<NewChunkData> newChunks, final int dimension) {
        try {
            String statement = "INSERT OR IGNORE INTO \"" + dimension + "\" VALUES ";
            statement += newChunks.stream()
                    .map(chunk -> "(" + chunk.x + ", " + chunk.z + ", " + chunk.foundTime + ")")
                    .collect(Collectors.joining(", "));
            connection.createStatement().execute(statement);
        } catch (Exception e) {
            XaeroPlus.LOGGER.error("Error inserting new chunks into database", e);
        }
    }

    public List<NewChunkData> getNewChunksInWindow(
            final int dimension,
            final int regionXMin, final int regionXMax,
            final int regionZMin, final int regionZMax) {
        try {
            ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM \"" + dimension + "\" "
                    + "WHERE x >= " + regionCoordToChunkCoord(regionXMin) + " AND x <= " + regionCoordToChunkCoord(regionXMax)
                    + " AND z >= " + regionCoordToChunkCoord(regionZMin) + " AND z <= " + regionCoordToChunkCoord(regionZMax));
            List<NewChunkData> newChunks = new ArrayList<>();
            while (resultSet.next()) {
                newChunks.add(new NewChunkData(
                        resultSet.getInt("x"),
                        resultSet.getInt("z"),
                        resultSet.getInt("foundTime")));
            }
            return newChunks;
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error while getting new chunks from database", e);
            // fall through
        }
        return Collections.emptyList();
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (Exception e) {
            XaeroPlus.LOGGER.warn("Failed closing NewChunks database connection", e);
        }
    }
}
