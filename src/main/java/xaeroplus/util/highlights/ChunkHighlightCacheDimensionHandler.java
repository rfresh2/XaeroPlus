package xaeroplus.util.highlights;

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

public class ChunkHighlightCacheDimensionHandler extends ChunkHighlightBaseCacheHandler {
    private final int dimension;
    private int windowRegionX = 0;
    private int windowRegionZ = 0;
    // square centered at windowX, windowZ with size windowSize
    private int windowRegionSize = 0;
    private final ChunkHighlightDatabase database;
    private final ListeningExecutorService executorService = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());

    public ChunkHighlightCacheDimensionHandler(final int dimension, final ChunkHighlightDatabase database) {
        this.dimension = dimension;
        this.database = database;
    }

    public void setWindow(int regionX, int regionZ, int regionSize) {
        this.windowRegionX = regionX;
        this.windowRegionZ = regionZ;
        this.windowRegionSize = regionSize;
        writeHighlightsOutsideWindowToDatabase();
        loadHighlightsInWindow();
    }

    private void loadHighlightsInWindow() {
        executorService.submit(() -> {
            final List<ChunkHighlightData> chunks = database.getHighlightsInWindow(
                    dimension,
                    windowRegionX - windowRegionSize, windowRegionX + windowRegionSize,
                    windowRegionZ - windowRegionSize, windowRegionZ + windowRegionSize
            );
            try {
                if (lock.writeLock().tryLock(1, TimeUnit.SECONDS)) {
                    for (final ChunkHighlightData chunk : chunks) {
                        this.chunks.put(chunkPosToLong(chunk.x, chunk.z), chunk.foundTime);
                    }
                    lock.writeLock().unlock();
                }
            } catch (final Exception e) {
                XaeroPlus.LOGGER.error("Failed to load highlights in window", e);
            }
        });
    }

    private void writeHighlightsOutsideWindowToDatabase() {
        executorService.execute(() -> {
            final List<ChunkHighlightData> chunksToWrite = new ArrayList<>();
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
                            chunksToWrite.add(new ChunkHighlightData(chunkX, chunkZ, entry.getLongValue()));
                            return true;
                        }
                        return false;
                    });
                    lock.writeLock().unlock();
                }
            } catch (final Exception e) {
                XaeroPlus.LOGGER.error("Error while writing highlights outside window to database", e);
            }
            database.insertHighlightList(chunksToWrite, dimension);
        });
    }

    public ListenableFuture<?> writeAllHighlightsToDatabase() {
        return executorService.submit(() -> {
            final List<ChunkHighlightData> chunksToWrite = new ArrayList<>();
            try {
                if (lock.readLock().tryLock(1, TimeUnit.SECONDS)) {
                    chunks.long2LongEntrySet().forEach(entry -> {
                        final long chunkPos = entry.getLongKey();
                        final int chunkX = ChunkUtils.longToChunkX(chunkPos);
                        final int chunkZ = ChunkUtils.longToChunkZ(chunkPos);
                        chunksToWrite.add(new ChunkHighlightData(chunkX, chunkZ, entry.getLongValue()));
                    });
                    lock.readLock().unlock();
                }
            } catch (final Exception e) {
                XaeroPlus.LOGGER.error("Error while writing all chunks to database", e);
            }
            database.insertHighlightList(chunksToWrite, dimension);
        });
    }

    @Override
    public void removeHighlight(final int x, final int z) {
        super.removeHighlight(x, z);
        database.removeHighlight(x, z, dimension);
    }

    public void close() {
        executorService.shutdown();
    }

    @Override
    public void handleWorldChange() {}

    @Override
    public void handleTick() {}

    @Override
    public void onEnable() {}

    @Override
    public void onDisable() {}
}
