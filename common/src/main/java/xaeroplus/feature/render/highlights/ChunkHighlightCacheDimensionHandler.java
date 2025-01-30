package xaeroplus.feature.render.highlights;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import it.unimi.dsi.fastutil.longs.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import xaeroplus.XaeroPlus;
import xaeroplus.event.XaeroWorldChangeEvent;
import xaeroplus.util.ChunkUtils;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.StampedLock;

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
    // newly added highlights we need to write back to the database
    // if a highlight is not in this set, we do not write it to the database
    // helps performance at very low zoom levels as most data is old and does not need to be rewritten constantly
    public final LongSet staleChunks = new LongOpenHashSet();
    public final ReadWriteLock staleChunksLock = new StampedLock().asReadWriteLock();
    ListenableFuture<?> windowMoveFuture = Futures.immediateVoidFuture();

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
        if (windowChanged
            && !windowMoveFuture.isDone()
            && (regionX != 0 || regionZ != 0 || regionSize != 0) // queue window change if we are clearing it (setting size to 0)
        ) {
            XaeroPlus.LOGGER.info("Rejecting window move to: [{} {} {}] from: [{} {} {}]", regionX, regionZ, regionSize, windowRegionX, windowRegionZ, windowRegionSize);
            return;
        }
        int prevWindowRegionX = windowRegionX;
        int prevWindowRegionZ = windowRegionZ;
        int prevWindowRegionSize = windowRegionSize;
        this.windowRegionX = regionX;
        this.windowRegionZ = regionZ;
        this.windowRegionSize = regionSize;
        if (windowChanged) {
            try {
                windowMoveFuture = executorService.submit(() ->
                      moveWindow0(regionX, regionZ, regionSize, prevWindowRegionX, prevWindowRegionZ, prevWindowRegionSize)
                );
            } catch (final Exception e) {
                XaeroPlus.LOGGER.error("Failed submitting move window task for {} disk cache dimension: {}", database.databaseName, dimension.location(), e);
            }
        }
    }

    private void moveWindow0(final int windowRegionX, final int windowRegionZ, final int windowRegionSize, final int prevWindowRegionX, final int prevWindowRegionZ, final int prevWindowRegionSize) {
        // load new data
        Long2LongMap dataBuf = new Long2LongOpenHashMap();
        database.getHighlightsInWindowAndOutsidePrevWindow(
            dimension,
            windowRegionX - windowRegionSize, windowRegionX + windowRegionSize,
            windowRegionZ - windowRegionSize, windowRegionZ + windowRegionSize,
            prevWindowRegionX - prevWindowRegionSize, prevWindowRegionX + prevWindowRegionSize,
            prevWindowRegionZ - prevWindowRegionSize, prevWindowRegionZ + prevWindowRegionSize,
            (chunkX, chunkZ, foundTime) -> dataBuf.put(chunkPosToLong(chunkX, chunkZ), foundTime)
        );
        try {
            // minimizes time we have to hold the lock by querying the database outside the lock's scope
            // at cost of a bit more memory
            if (!dataBuf.isEmpty() && lock.writeLock().tryLock(1, TimeUnit.SECONDS)) {
                for (var entry : Long2LongMaps.fastIterable(dataBuf)) {
                    chunks.put(entry.getLongKey(), entry.getLongValue());
                }
                lock.writeLock().unlock();
            }
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Failed to load highlights in window for {} disk cache dimension: {}", database.databaseName, dimension.location(), e);
        }

        dataBuf.clear();
        // write to db and remove data from local cache outside window
        var chunkXMin = regionCoordToChunkCoord(windowRegionX - windowRegionSize);
        var chunkXMax = regionCoordToChunkCoord(windowRegionX + windowRegionSize);
        var chunkZMin = regionCoordToChunkCoord(windowRegionZ - windowRegionSize);
        var chunkZMax = regionCoordToChunkCoord(windowRegionZ + windowRegionSize);
        try {
            if (staleChunksLock.readLock().tryLock(1, TimeUnit.SECONDS)) {
                if (!chunks.isEmpty() && lock.readLock().tryLock(1, TimeUnit.SECONDS)) {
                    for (var it = Long2LongMaps.fastIterator(chunks); it.hasNext(); ) {
                        var entry = it.next();
                        final long chunkPos = entry.getLongKey();
                        final int chunkX = ChunkUtils.longToChunkX(chunkPos);
                        final int chunkZ = ChunkUtils.longToChunkZ(chunkPos);
                        if (chunkX < chunkXMin
                            || chunkX > chunkXMax
                            || chunkZ < chunkZMin
                            || chunkZ > chunkZMax) {
                            dataBuf.put(chunkPos, entry.getLongValue());
                        }
                    }
                    lock.readLock().unlock();
                }
                staleChunksLock.readLock().unlock();
                if (!dataBuf.isEmpty() && lock.writeLock().tryLock(1L, TimeUnit.SECONDS)) {
                    for (var it = Long2LongMaps.fastIterator(dataBuf); it.hasNext(); ) {
                        chunks.remove(it.next().getLongKey());
                    }
                    lock.writeLock().unlock();
                }
            }
            if (!dataBuf.isEmpty() && staleChunksLock.writeLock().tryLock(1, TimeUnit.SECONDS)) {
                for (var it = Long2LongMaps.fastIterator(dataBuf); it.hasNext(); ) {
                    long chunkPos = it.next().getLongKey();
                    if (staleChunks.contains(chunkPos)) {
                        // retain in buf to be written to db
                        staleChunks.remove(chunkPos);
                    } else {
                        it.remove();
                    }
                }
                staleChunksLock.writeLock().unlock();
            }
            database.insertHighlightList(dataBuf, dimension);
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error while writing highlights outside window to {} disk cache dimension: {}", database.databaseName, dimension.location(), e);
        }
    }

    public ListenableFuture<Long2LongMap> getHighlightsInCustomWindow(int windowRegionX, int windowRegionZ, int windowRegionSize) {
        try {
            return executorService.submit(() -> getHighlightsInCustomWindow0(windowRegionX, windowRegionZ, windowRegionSize));
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Failed submitting load highlights task for {} disk cache dimension: {}", database.databaseName, dimension.location(), e);
            return Futures.immediateFuture(Long2LongMaps.EMPTY_MAP);
        }
    }

    private @NotNull Long2LongOpenHashMap getHighlightsInCustomWindow0(final int windowRegionX, final int windowRegionZ, final int windowRegionSize) {
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
                for (var entry : Long2LongMaps.fastIterable(chunks)) {
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
                lock.readLock().unlock();
            }
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Failed to load highlights in custom window for {} disk cache dimension: {}", database.databaseName, dimension.location(), e);
        }
        return map;
    }

    // does not remove from local cache
    public ListenableFuture<?> writeStaleHighlightsToDatabase() {
        try {
            return executorService.submit(this::writeStaleHighlightsToDatabase0);
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Failed submitting write all highlights task for {} disk cache dimension: {}", database.databaseName, dimension.location(), e);
            return Futures.immediateFuture(null);
        }
    }

    private void writeStaleHighlightsToDatabase0() {
        if (staleChunks.isEmpty()) return;
        Long2LongMap chunksToWrite = new Long2LongOpenHashMap();
        try {
            LongSet hangingStaleChunks = new LongOpenHashSet();
            if (staleChunksLock.readLock().tryLock(1, TimeUnit.SECONDS)) {
                if (lock.readLock().tryLock(1L, TimeUnit.SECONDS)) {
                    for (long chunkPos : staleChunks) {
                        long foundTime = chunks.get(chunkPos);
                        if (foundTime != chunks.defaultReturnValue()) {
                            chunksToWrite.put(chunkPos, foundTime);
                        } else {
                            hangingStaleChunks.add(chunkPos);
                        }
                    }
                    lock.readLock().unlock();
                }
                staleChunksLock.readLock().unlock();
            }
            if (staleChunksLock.writeLock().tryLock(1, TimeUnit.SECONDS)) {
                for (var entry : Long2LongMaps.fastIterable(chunksToWrite)) {
                    staleChunks.remove(entry.getLongKey());
                }
                staleChunks.removeAll(hangingStaleChunks);
                staleChunksLock.writeLock().unlock();
            }
            database.insertHighlightList(chunksToWrite, dimension);
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error while writing all highlights to {} disk cache dimension: {}", database.databaseName, dimension.location(), e);
        }
    }

    @Override
    public boolean addHighlight(final int x, final int z) {
        boolean b = super.addHighlight(x, z, System.currentTimeMillis());
        if (b) {
            try {
                if (staleChunksLock.writeLock().tryLock(1, TimeUnit.SECONDS)) {
                    staleChunks.add(chunkPosToLong(x, z));
                    staleChunksLock.writeLock().unlock();
                }
            } catch (final Exception e) {
                XaeroPlus.LOGGER.error("Failed to add highlight to {} stale chunks: {}", database.databaseName, dimension.location(), e);
            }
        }
        return b;
    }

    @Override
    public void loadPreviousState(final Long2LongMap state) {
        super.loadPreviousState(state);
        try {
            if (staleChunksLock.writeLock().tryLock(1, TimeUnit.SECONDS)) {
                staleChunks.clear();
                if (lock.readLock().tryLock(1, TimeUnit.SECONDS)) {
                    for (var it = Long2LongMaps.fastIterator(chunks); it.hasNext(); ) {
                        staleChunks.add(it.next().getLongKey());
                    }
                    lock.readLock().unlock();
                }
                staleChunksLock.writeLock().unlock();
            }
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Failed to load previous state for {} stale chunks: {}", database.databaseName, dimension.location(), e);
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
