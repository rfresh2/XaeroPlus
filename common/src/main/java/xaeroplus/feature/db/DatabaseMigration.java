package xaeroplus.feature.db;

import org.rfresh.sqlite.SQLiteErrorCode;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public interface DatabaseMigration {
    boolean shouldMigrate(String databaseName, Connection connection, boolean init) throws SQLException;
    void doMigration(String databaseName, Connection connection, boolean init) throws SQLException, InterruptedException;

    static boolean isCorruptDatabase(final Throwable throwable) {
        var current = throwable;
        while (current != null) {
            if (current instanceof SQLException sqlException
                && sqlException.getErrorCode() == SQLiteErrorCode.SQLITE_CORRUPT.code) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }


    static void executeCancellable(final Connection connection, final String sql) throws SQLException, InterruptedException {
        try (var statement = connection.createStatement()) {
            var future = CompletableFuture.runAsync(() -> {
                try {
                    statement.execute(sql);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
            while (!future.isDone()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    statement.cancel();
                    throw e;
                }
            }
            if (future.isCompletedExceptionally()) {
                try {
                    future.join();
                } catch (CancellationException ignored) {

                } catch (CompletionException e) {
                    var cause = e.getCause();
                    if (cause instanceof SQLException sqlException) {
                        throw sqlException;
                    }
                    if (cause instanceof InterruptedException interruptedException) {
                        throw interruptedException;
                    }
                    throw e;
                }
            }
        }
    }
}
