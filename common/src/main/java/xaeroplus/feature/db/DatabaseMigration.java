package xaeroplus.feature.db;

import java.sql.Connection;

public interface DatabaseMigration {
    boolean shouldMigrate(String databaseName, Connection connection);
    void doMigration(String databaseName, Connection connection);
}
