package xaeroplus.feature.render.highlights;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import it.unimi.dsi.fastutil.longs.Long2LongArrayMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongMaps;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import xaeroplus.XaeroPlus;
import xaeroplus.event.XaeroWorldChangeEvent;
import xaeroplus.util.ChunkUtils;

import java.util.concurrent.TimeUnit;

import static xaeroplus.util.ChunkUtils.chunkPosToLong;
import static xaeroplus.util.ChunkUtils.regionCoordToChunkCoord;

public class ChunkHighlightCacheDimensionHandler extends ChunkHighlightBaseCacheHandler {
    @NotNull private final ResourceKey<Level> dimension;
    private int windowRegionX = 0;
    private int windowRegionZ = 0;
    // square centered at windowX, windowZ with size windowSize
    private int windowRegionSize = 0;
    @NotNull private final ChunkHighlightDatabase database;
    @NotNull private final ListeningExecutorService executorService;

    public ChunkHighlightCacheDimensionHandler(
        @NotNull ResourceKey<Level> dimension,
        @NotNull ChunkHighlightDatabase database,
        @NotNull ListeningExecutorService executorService) {
        this.dimension = dimension;
        this.database = database;
        this.executorService = executorService;
    }

    public void setWindow(int regionX, int regionZ, int regionSize) {
        boolean windowChanged = regionX != windowRegionX || regionZ != windowRegionZ || regionSize != windowRegionSize;
        this.windowRegionX = regionX;
        this.windowRegionZ = regionZ;
        this.windowRegionSize = regionSize;
        if (windowChanged) {
            writeHighlightsOutsideWindowToDatabase();
            loadHighlightsInWindow();
        }
    }

    private void loadHighlightsInWindow() {
        try {
            executorService.execute(() -> {
                Long2LongMap data = new Long2LongOpenHashMap();
                database.getHighlightsInWindow(
                    dimension,
                    windowRegionX - windowRegionSize, windowRegionX + windowRegionSize,
                    windowRegionZ - windowRegionSize, windowRegionZ + windowRegionSize,
                    (chunkX, chunkZ, foundTime) -> data.put(chunkPosToLong(chunkX, chunkZ), foundTime)
                );
                try {
                    // minimizes time we have to hold the lock by querying the database outside the lock's scope
                    // at cost of a bit more memory
                    if (lock.writeLock().tryLock(1, TimeUnit.SECONDS)) {
                        this.chunks.putAll(data);
                        lock.writeLock().unlock();
                    }
                } catch (final Exception e) {
                    XaeroPlus.LOGGER.error("Failed to load highlights in window for {} disk cache dimension: {}", database.databaseName, dimension.location(), e);
                }
            });
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Failed submitting load highlights task for {} disk cache dimension: {}", database.databaseName, dimension.location(), e);
        }
    }

    public ListenableFuture<Long2LongMap> getHighlightsInCustomWindow(int windowRegionX, int windowRegionZ, int windowRegionSize) {
        try {
            return executorService.submit(() -> {
                var map = new Long2LongOpenHashMap();
                int regionXMin = windowRegionX - windowRegionSize;
                int regionZMin = windowRegionZ - windowRegionSize;
                int regionXMax = windowRegionX + windowRegionSize;
                int regionZMax = windowRegionZ + windowRegionSize;
                database.getHighlightsInWindow(
                    dimension,
                    regionXMin, regionXMax,
                    regionZMin, regionZMax,
                    (chunkX, chunkZ, foundTime) -> map.put(chunkPosToLong(chunkX, chunkZ), foundTime)
                );
                // append chunks from local cache
                try {
                    int chunkXMin = regionCoordToChunkCoord(regionXMin);
                    int chunkXMax = regionCoordToChunkCoord(regionXMax);
                    int chunkZMin = regionCoordToChunkCoord(regionZMin);
                    int chunkZMax = regionCoordToChunkCoord(regionZMax);
                    if (lock.readLock().tryLock(1, TimeUnit.SECONDS)) {
                        for (var entry : chunks.long2LongEntrySet()) {
                            final long chunkPos = entry.getLongKey();
                            final int chunkX = ChunkUtils.longToChunkX(chunkPos);
                            final int chunkZ = ChunkUtils.longToChunkZ(chunkPos);
                            if (chunkX >= chunkXMin
                                && chunkX <= chunkXMax
                                && chunkZ >= chunkZMin
                                && chunkZ <= chunkZMax) {
                                map.put(chunkPos, entry.getLongValue());
                            }
                        }
                    }
                } catch (final Exception e) {
                    XaeroPlus.LOGGER.error("Failed to load highlights in custom window for {} disk cache dimension: {}", database.databaseName, dimension.location(), e);
                }
                return map;
            });
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Failed submitting load highlights task for {} disk cache dimension: {}", database.databaseName, dimension.location(), e);
            return Futures.immediateFuture(Long2LongMaps.EMPTY_MAP);
        }
    }

    // removes chunks outside window from local cache and writes them to database
    private void writeHighlightsOutsideWindowToDatabase() {
        try {
            executorService.execute(() -> {
                final Long2LongMap chunksToWrite = new Long2LongOpenHashMap();
                try {
                    if (lock.writeLock().tryLock(1L, TimeUnit.SECONDS)) {
                        var minChunkX = regionCoordToChunkCoord(windowRegionX - windowRegionSize);
                        var maxChunkX = regionCoordToChunkCoord(windowRegionX + windowRegionSize);
                        var minChunkZ = regionCoordToChunkCoord(windowRegionZ - windowRegionSize);
                        var maxChunkZ = regionCoordToChunkCoord(windowRegionZ + windowRegionSize);
                        for (var it = Long2LongMaps.fastIterator(chunks); it.hasNext(); ) {
                            var entry = it.next();
                            final long chunkPos = entry.getLongKey();
                            final int chunkX = ChunkUtils.longToChunkX(chunkPos);
                            final int chunkZ = ChunkUtils.longToChunkZ(chunkPos);
                            if (chunkX < minChunkX
                                || chunkX > maxChunkX
                                || chunkZ < minChunkZ
                                || chunkZ > maxChunkZ) {
                                chunksToWrite.put(chunkPos, entry.getLongValue());
                                it.remove();
                            }
                        }
                        lock.writeLock().unlock();
                    }
                } catch (final Exception e) {
                    XaeroPlus.LOGGER.error("Error while writing highlights outside window to {} disk cache dimension: {}", database.databaseName, dimension.location(), e);
                }
                database.insertHighlightList(chunksToWrite, dimension);
            });
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Failed submitting write highlights task for {} disk cache dimension: {}", database.databaseName, dimension.location(), e);
        }
    }

    // does not remove from local cache
    public ListenableFuture<?> writeAllHighlightsToDatabase() {
        try {
            return executorService.submit(() -> {
                Long2LongMap chunksToWrite = Long2LongMaps.EMPTY_MAP;
                try {
                    if (lock.readLock().tryLock(1, TimeUnit.SECONDS)) {
                        // we are simply going to iterate through all entries anyway
                        // no need for a hashing map
                        chunksToWrite = new Long2LongArrayMap(chunks);
                        lock.readLock().unlock();
                    }
                } catch (final Exception e) {
                    XaeroPlus.LOGGER.error("Error while writing all chunks to {} disk cache dimension: {}", database.databaseName, dimension.location(), e);
                }
                database.insertHighlightList(chunksToWrite, dimension);
            });
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Failed submitting write all highlights task for {} disk cache dimension: {}", database.databaseName, dimension.location(), e);
            return Futures.immediateFuture(null);
        }
    }

    @Override
    public boolean removeHighlight(final int x, final int z) {
        super.removeHighlight(x, z);
        try {
            database.removeHighlight(x, z, dimension);
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Failed to remove highlight from {} disk cache dimension: {}", database.databaseName, dimension.location(), e);
            return false;
        }
        return true;
    }

    @Override
    public void handleWorldChange(final XaeroWorldChangeEvent event) {}

    @Override
    public void handleTick() {}

    @Override
    public void onEnable() {}

    @Override
    public void onDisable() {}
}
