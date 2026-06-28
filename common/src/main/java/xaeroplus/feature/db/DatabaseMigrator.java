package xaeroplus.feature.db;

import net.lenni0451.lambdaevents.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import org.rfresh.sqlite.SQLiteConnection;
import xaeroplus.Globals;
import xaeroplus.XaeroPlus;
import xaeroplus.event.ClientTickEvent;
import xaeroplus.event.XaeroWorldChangeEvent;
import xaeroplus.util.NotificationUtil;

import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class DatabaseMigrator {
    private static final MigrationMonitor MIGRATION_MONITOR = new MigrationMonitor();
    private static final Semaphore HEAVY_OPERATION_PERMITS = new Semaphore(1, true);
    private final List<DatabaseMigration> migrations;

    public DatabaseMigrator(final List<DatabaseMigration> migrations) {
        this.migrations = migrations;
    }

    public Connection migrate(Path dbPath, String databaseName, Connection connection, final boolean init) throws Exception {
        var recoveryAttempted = new AtomicBoolean(false);
        while (true) { // to allow corrupt db recovery to attempt migrations again
            try {
                if (init) {
                    return migrateInit(dbPath, databaseName, connection);
                } else {
                    return migrateExisting(dbPath, databaseName, connection);
                }
            } catch (final Exception e) {
                MIGRATION_MONITOR.onMigrationEnd(databaseName, false);
                if (DatabaseMigration.isCorruptDatabase(e) && recoveryAttempted.compareAndSet(false, true)) {
                    XaeroPlus.LOGGER.error("Corruption detected in {} database", databaseName, e);
                    connection = recoverCorruptDatabase(databaseName, dbPath, connection);
                    NotificationUtil.inGameNotification("Database: " + databaseName + " recovered successfully! Retrying migration...");
                    continue;
                }
                XaeroPlus.LOGGER.error("Failed migrating database: {}", databaseName, e);
                NotificationUtil.inGameNotification("Database: " + databaseName + " failed to migrate!");
                NotificationUtil.inGameNotification("More info will be in your log");
                throw e;
            }
        }
    }

    private Connection migrateExisting(final Path dbPath, String databaseName, Connection connection) throws SQLException, InterruptedException {
        for (var migration : migrations) {
            if (migration.shouldMigrate(databaseName, connection)) {
                long beforeMigration = System.nanoTime();
                XaeroPlus.LOGGER.info("Found database: {} that needs migration", databaseName);
                MIGRATION_MONITOR.onMigrationStart(databaseName);
                executeConcurrencyLimited(databaseName, "migration", () -> {
                    validateAvailableDiskSpace(dbPath);
                    long beforeBackup = System.nanoTime();
                    var backupPath = backupDatabase(dbPath, databaseName, connection);
                    long afterBackup = System.nanoTime();
                    XaeroPlus.LOGGER.info("Backed up database: {} to {} in {} ms", databaseName, backupPath, (afterBackup - beforeBackup) / 1000000);
                    long beforeRunMigration = System.nanoTime();
                    runMigration(databaseName, connection, migration);
                    long afterRunMigration = System.nanoTime();
                    XaeroPlus.LOGGER.info("Ran migration: {} in {} ms", migration.getClass().getSimpleName(), (afterRunMigration - beforeRunMigration) / 1000000);
                    long beforeVacuum = System.nanoTime();
                    vacuum(connection);
                    long afterVacuum = System.nanoTime();
                    XaeroPlus.LOGGER.info("Vacuumed database: {} in {} ms", databaseName, (afterVacuum - beforeVacuum) / 1000000);
                });
                long afterMigration = System.nanoTime();
                XaeroPlus.LOGGER.info("completed {} migration duration in {} ms", databaseName, (afterMigration - beforeMigration) / 1000000);
                MIGRATION_MONITOR.onMigrationEnd(databaseName, true);
            }
        }
        return connection;
    }

    private Connection migrateInit(final Path dbPath, String databaseName, Connection connection) throws SQLException, InterruptedException {
        for (var migration : migrations) {
            if (migration.shouldMigrate(databaseName, connection)) {
                runMigration(databaseName, connection, migration);
            }
        }
        return connection;
    }

    private void runMigration(
        final String databaseName,
        final Connection connection,
        final DatabaseMigration migration
    ) throws SQLException, InterruptedException {
        var committed = false;
        try {
            connection.setAutoCommit(false);
            migration.doMigration(databaseName, connection);
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Migration interrupted");
            }
            connection.commit();
            committed = true;
        } finally {
            try {
                if (!committed) {
                    connection.rollback();
                }
                connection.setAutoCommit(true);
            } catch (final SQLException e) {
                if (!e.getMessage().contains("no transaction is active")) {
                    XaeroPlus.LOGGER.error("Failed rolling back migration for database: {}", databaseName, e);
                }
            }
        }
    }

    private void executeConcurrencyLimited(
        final String databaseName,
        final String operation,
        final SqlOperation sqlOperation
    ) throws SQLException, InterruptedException {
        var permitAcquired = HEAVY_OPERATION_PERMITS.tryAcquire(1, TimeUnit.HOURS);
        if (!permitAcquired) {
            throw new RuntimeException("Failed to acquire permit for database " + operation + ": " + databaseName);
        }
        try {
            sqlOperation.run();
        } finally {
            HEAVY_OPERATION_PERMITS.release();
        }
    }

    private Path getBackupPath(Path dbPath) {
        return dbPath.getParent().resolve("XaeroPlus-db-backups");
    }

    public String backupDatabase(Path dbPath, String databaseName, Connection connection) throws SQLException, InterruptedException {
        Path backupPath = getBackupPath(dbPath);
        if (!backupPath.toFile().exists()) {
            backupPath.toFile().mkdirs();
        }
        String dbBackupLocation = backupPath.resolve(databaseName + "-" + Instant.now().toEpochMilli() + ".db").toString();
        DatabaseMigration.executeCancellable(connection, "BACKUP TO '" + dbBackupLocation + "'");
        return dbBackupLocation;
    }

    private void vacuum(final Connection connection) throws SQLException, InterruptedException {
        DatabaseMigration.executeCancellable(connection, "VACUUM");
    }

    // this can take an extremely long time for large databases
    private Connection recoverCorruptDatabase(String databaseName, Path dbPath, Connection connection) throws Exception {
        NotificationUtil.inGameNotification("Database: " + databaseName + " is corrupt! Attempting to recover...");
        XaeroPlus.LOGGER.info("Attempting to recover corrupt database: {}", databaseName);
        final Path recoveredDbPath = dbPath.getParent().resolve("recovered_" + databaseName + "-" + System.currentTimeMillis() + ".db");

        try {
            DatabaseMigration.executeCancellable(connection, "recover to \"" + recoveredDbPath.toAbsolutePath() + "\"");
            XaeroPlus.LOGGER.info("Wrote recovered database to: {}", recoveredDbPath);
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error recovering corrupt database: {}", databaseName, e);
            NotificationUtil.inGameNotification("Database: " + databaseName + " failed to recover!");
            throw e;
        }
        try {
            connection.close();
            XaeroPlus.LOGGER.info("Closed DB connection to corrupt database: {}", databaseName);
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error closing connection to corrupt database: {}", databaseName, e);
            throw e;
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
            if (connection instanceof SQLiteConnection sqliteConnection) {
                sqliteConnection.setBusyTimeout(5000);
            }
            XaeroPlus.LOGGER.info("Opened DB connection to recovered database: {}", databaseName);
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error reopening connection to recovered database: {}", databaseName, e);
            throw e;
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
        return connection;
    }

    private void validateAvailableDiskSpace(Path dbPath) {
        if (!dbPath.toFile().exists()) return;
        try {
            var dbSize = Files.size(dbPath);
            long freeSpace = dbPath.getParent().toFile().getUsableSpace();
            if (freeSpace < dbSize * 3) {
                throw new RuntimeException("Not enough available disk space to migrate database: " + dbPath.toFile().getName());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to check available disk space for database: " + dbPath.toFile().getName(), e);
        }
    }

    @FunctionalInterface
    private interface SqlOperation {
        void run() throws SQLException, InterruptedException;
    }

    public static class MigrationMonitor {
        private final Map<String, MigrationStatus> migrations = new HashMap<>();
        private final SystemToast.SystemToastId toastId = new SystemToast.SystemToastId();

        public MigrationMonitor() {
            XaeroPlus.EVENT_BUS.register(this);
        }

        public synchronized void onMigrationStart(String id) {
            migrations.put(id, MigrationStatus.IN_PROGRESS);
        }

        public synchronized void onMigrationEnd(String id, boolean success) {
            if (migrations.containsKey(id)) {
                migrations.put(id, success ? MigrationStatus.COMPLETED : MigrationStatus.FAILED);
            }
        }

        synchronized void reset() {
            migrations.clear();
            toastActive = false;
        }

        boolean toastActive = false;

        @EventHandler
        public synchronized void onTick(ClientTickEvent.Pre event) {
            var mc = Minecraft.getInstance();
            if (mc.player == null) return;
            int inProgressMigrations = 0;
            int completedMigrations = 0;
            int failedMigrations = 0;
            for (var status : migrations.values()) {
                switch (status) {
                    case IN_PROGRESS -> inProgressMigrations++;
                    case COMPLETED -> completedMigrations++;
                    case FAILED -> failedMigrations++;
                }
            }
            if (inProgressMigrations <= 0) {
                if (toastActive) {
                    SystemToast.addOrUpdate(mc.gui.toastManager(), toastId,
                        Component.literal("XaeroPlus"),
                        Component.translatable("xaeroplus.gui.toast.database_migration_done", completedMigrations, failedMigrations)
                    );
                }
                toastActive = false;
                return;
            }
            SystemToast.addOrUpdate(mc.gui.toastManager(), toastId,
                Component.literal("XaeroPlus"),
                Component.translatable("xaeroplus.gui.toast.database_migration_in_progress", inProgressMigrations, completedMigrations, failedMigrations)
            );
            toastActive = true;
        }

        @EventHandler(priority = 1000)
        public synchronized void onWorldChange(XaeroWorldChangeEvent event) {
            switch (event.worldChangeType()) {
                case EXIT_WORLD, ENTER_WORLD -> {
                    reset();
                }
            }
        }

        enum MigrationStatus {
            IN_PROGRESS, COMPLETED, FAILED
        }
    }
}
