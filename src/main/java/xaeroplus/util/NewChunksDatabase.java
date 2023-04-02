package xaeroplus.util;

import xaeroplus.XaeroPlus;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class NewChunksDatabase {
    private final Connection connection;

    public NewChunksDatabase(String worldId) {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:XaeroWorldMap/" + worldId + "/XaeroPlusNewChunks.db");
            createNewChunksTable();
        } catch (Exception e) {
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

    public void insertNewChunk(int x, int z, long foundTime, int dimension) {
        try {
            connection.createStatement().execute("INSERT OR IGNORE INTO \"" + dimension + "\" VALUES (" + x + ", " + z + ", " + foundTime + ")");
        } catch (Exception e) {
            XaeroPlus.LOGGER.error("Error while inserting new chunk into database", e);
        }
    }

    public void insertNewChunkList(final List<NewChunkData> newChunks, final int dimension) {
        try {
            String statement = "INSERT OR IGNORE INTO \"" + dimension + "\" VALUES ";
            statement += newChunks.stream()
                    .map(chunk -> "(" + chunk.x + ", " + chunk.z + ", " + chunk.foundTime + ")")
                    .collect(Collectors.joining(", "));
            connection.createStatement().execute(statement);
            XaeroPlus.LOGGER.info("Saved {} NewChunks to db", newChunks.size());
        } catch (Exception e) {
            XaeroPlus.LOGGER.error("Error inserting new chunks into database", e);
        }
    }

    public List<NewChunkData> getNewChunks(int dimension) {
        try {
            ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM \"" + dimension + "\"");
            List<NewChunkData> newChunks = new ArrayList<>();
            while (resultSet.next()) {
                newChunks.add(new NewChunkData(resultSet.getInt("x"),
                        resultSet.getInt("z"),
                        resultSet.getInt("foundTime")));
            }
            return newChunks;
        } catch (Exception e) {
            XaeroPlus.LOGGER.error("Error while getting new chunks from database", e);
            // fall through
        }
        return Collections.emptyList();
    }

}
