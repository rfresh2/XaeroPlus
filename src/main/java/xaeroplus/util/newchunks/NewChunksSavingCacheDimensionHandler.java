package xaeroplus.util.newchunks;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import xaeroplus.XaeroPlus;
import xaeroplus.util.ChunkUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static xaeroplus.util.ChunkUtils.chunkPosToLong;
import static xaeroplus.util.ChunkUtils.regionCoordToChunkCoord;

public class NewChunksSavingCacheDimensionHandler extends NewChunksBaseCacheHandler {
    private final int dimension;
    private int windowRegionX = 0;
    private int windowRegionZ = 0;
    // square centered at windowX, windowZ with size windowSize
    private int windowRegionSize = 0;
    private final NewChunksDatabase database;
    private final ListeningExecutorService executorService = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());

    public NewChunksSavingCacheDimensionHandler(final int dimension, final NewChunksDatabase database) {
        this.dimension = dimension;
        this.database = database;
    }

    public void setWindow(int regionX, int regionZ, int regionSize) {
        this.windowRegionX = regionX;
        this.windowRegionZ = regionZ;
        this.windowRegionSize = regionSize;
        writeNewChunksOutsideWindowToDatabase();
        loadNewChunksInWindow();
    }

    private void loadNewChunksInWindow() {
        executorService.submit(() -> {
            final List<NewChunkData> newChunks = database.getNewChunksInWindow(dimension,
                    windowRegionX - windowRegionSize, windowRegionX + windowRegionSize,
                    windowRegionZ - windowRegionSize, windowRegionZ + windowRegionSize
            );
            try {
                if (lock.writeLock().tryLock(1, TimeUnit.SECONDS)) {
                    for (final NewChunkData chunk : newChunks) {
                        chunks.put(chunkPosToLong(chunk.x, chunk.z), chunk.foundTime);
                    }
                    lock.writeLock().unlock();
                }
            } catch (final Exception e) {
                XaeroPlus.LOGGER.error("Failed to load new chunks in window", e);
            }
        });
    }

    private void writeNewChunksOutsideWindowToDatabase() {
        executorService.execute(() -> {
            final List<NewChunkData> chunksToWrite = new ArrayList<>();
            try {
                if (lock.writeLock().tryLock(1L, TimeUnit.SECONDS)) {
                    chunks.long2LongEntrySet().removeIf(entry -> {
                        final long chunkPos = entry.getLongKey();
                        final int chunkX = ChunkUtils.longToChunkX(chunkPos);
                        final int chunkZ = ChunkUtils.longToChunkZ(chunkPos);
                        if (chunkX < regionCoordToChunkCoord(windowRegionX - windowRegionSize)
                                || chunkX > regionCoordToChunkCoord(windowRegionX + windowRegionSize)
                                || chunkZ < regionCoordToChunkCoord(windowRegionZ - windowRegionSize)
                                || chunkZ > regionCoordToChunkCoord(windowRegionZ + windowRegionSize)) {
                            chunksToWrite.add(new NewChunkData(chunkX, chunkZ, entry.getLongValue()));
                            return true;
                        }
                        return false;
                    });
                    lock.writeLock().unlock();
                }
            } catch (final Exception e) {
                XaeroPlus.LOGGER.error("Error while writing new chunks outside window to database", e);
            }
            database.insertNewChunkList(chunksToWrite, dimension);
        });
    }

    public ListenableFuture<?> writeAllChunksToDatabase() {
        return executorService.submit(() -> {
            final List<NewChunkData> chunksToWrite = new ArrayList<>();
            try {
                if (lock.readLock().tryLock(1, TimeUnit.SECONDS)) {
                    chunks.long2LongEntrySet().forEach(entry -> {
                        final long chunkPos = entry.getLongKey();
                        final int chunkX = ChunkUtils.longToChunkX(chunkPos);
                        final int chunkZ = ChunkUtils.longToChunkZ(chunkPos);
                        chunksToWrite.add(new NewChunkData(chunkX, chunkZ, entry.getLongValue()));
                    });
                    lock.readLock().unlock();
                }
            } catch (final Exception e) {
                XaeroPlus.LOGGER.error("Error while writing all chunks to database", e);
            }
            database.insertNewChunkList(chunksToWrite, dimension);
        });
    }

    public void close() {
        executorService.shutdown();
    }

}
