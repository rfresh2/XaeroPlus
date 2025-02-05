package xaeroplus.feature.render.highlights;

import com.google.common.util.concurrent.FutureCallback;
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

import java.util.concurrent.CompletableFuture;

import static xaeroplus.util.ChunkUtils.chunkPosToLong;
import static xaeroplus.util.ChunkUtils.regionCoordToChunkCoord;

public class ChunkHighlightCacheDimensionHandler extends ChunkHighlightBaseCacheHandler {
    @NotNull private final ResourceKey<Level> dimension;
    private int windowRegionX = 0;
    private int windowRegionZ = 0;
    // square centered at windowX, windowZ with size windowSize
    private int windowRegionSize = 0;
    @NotNull private final ChunkHighlightDatabase database;
    @NotNull private final ListeningExecutorService dbExecutor;
    // newly added highlights we need to write back to the database
    // if a highlight is not in this set, we do not write it to the database
    // helps performance at very low zoom levels as most data is old and does not need to be rewritten constantly
    public final LongSet staleChunks = new LongOpenHashSet();
    ListenableFuture<?> windowMoveFuture = Futures.immediateVoidFuture();

    public ChunkHighlightCacheDimensionHandler(
        @NotNull ResourceKey<Level> dimension,
        @NotNull ChunkHighlightDatabase database,
        @NotNull ListeningExecutorService dbExecutor) {
        super();
        this.dimension = dimension;
        this.database = database;
        this.dbExecutor = dbExecutor;
    }

    public synchronized void setWindow(int regionX, int regionZ, int regionSize) {
        boolean windowChanged = regionX != windowRegionX || regionZ != windowRegionZ || regionSize != windowRegionSize;
        if (windowChanged
            && !windowMoveFuture.isDone()
            && (regionX != 0 || regionZ != 0 || regionSize != 0) // queue window change if we are clearing it (setting size to 0)
        ) {
            XaeroPlus.LOGGER.debug("Rejecting window move to: [{} {} {}] from: [{} {} {}]", regionX, regionZ, regionSize, windowRegionX, windowRegionZ, windowRegionSize);
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
                windowMoveFuture = moveWindow0(regionX, regionZ, regionSize, prevWindowRegionX, prevWindowRegionZ, prevWindowRegionSize);
            } catch (final Exception e) {
                XaeroPlus.LOGGER.error("Failed submitting move window task for {} disk cache dimension: {}", database.databaseName, dimension.location(), e);
            }
        }
    }

    private ListenableFuture<?> moveWindow0(final int windowRegionX, final int windowRegionZ, final int windowRegionSize, final int prevWindowRegionX, final int prevWindowRegionZ, final int prevWindowRegionSize) {
        ListenableFuture<Long2LongMap> loadDataFuture = dbExecutor.submit(() -> loadUpdatedWindowFromDatabase(windowRegionX, windowRegionZ, windowRegionSize, prevWindowRegionX, prevWindowRegionZ, prevWindowRegionSize));
        Futures.addCallback(loadDataFuture, new WindowDataLoadFutureCallback(), mc);
        ListenableFuture<?> removeDataFuture = flushChunksOutsideWindow(windowRegionX, windowRegionZ, windowRegionSize);
        return Futures.allAsList(loadDataFuture, removeDataFuture);
    }


    private Long2LongMap loadUpdatedWindowFromDatabase(final int windowRegionX, final int windowRegionZ, final int windowRegionSize, final int prevWindowRegionX, final int prevWindowRegionZ, final int prevWindowRegionSize) {
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
        return dataBuf;
    }

    private ListenableFuture<?> flushChunksOutsideWindow(final int windowRegionX, final int windowRegionZ, final int windowRegionSize) {
        if (!mc.isSameThread()) {
            throw new RuntimeException("removeChunksOutsideWindow must be called on the main thread");
        }
        Long2LongMap dataBuf = new Long2LongOpenHashMap();
        // write to db and remove data from local cache outside window
        var chunkXMin = regionCoordToChunkCoord(windowRegionX - windowRegionSize);
        var chunkXMax = regionCoordToChunkCoord(windowRegionX + windowRegionSize);
        var chunkZMin = regionCoordToChunkCoord(windowRegionZ - windowRegionSize);
        var chunkZMax = regionCoordToChunkCoord(windowRegionZ + windowRegionSize);
        // critical section in mc client tick thread
        // it would have huge benefits if we could optimize this
        // there is no cap on the size of the chunks map
        // so this can require iterating through millions of entries
        for (var it = chunks.keySet().longIterator(); it.hasNext(); ) {
            var chunkPos = it.nextLong();
            final int chunkX = ChunkUtils.longToChunkX(chunkPos);
            final int chunkZ = ChunkUtils.longToChunkZ(chunkPos);
            if (chunkX < chunkXMin
                || chunkX > chunkXMax
                || chunkZ < chunkZMin
                || chunkZ > chunkZMax) {
                if (staleChunks.contains(chunkPos)) {
                    dataBuf.put(chunkPos, chunks.get(chunkPos));
                }
                it.remove();
            }
        }
        return dbExecutor.submit(() -> database.insertHighlightList(dataBuf, dimension));
    }

    public Long2LongMap collectStaleHighlightsToWrite() {
        if (!mc.isSameThread()) {
            throw new RuntimeException("collectStaleHighlightsToWrite must be called on the main thread");
        }
        if (staleChunks.isEmpty()) return Long2LongMaps.EMPTY_MAP;
        Long2LongMap chunksToWrite = new Long2LongOpenHashMap(staleChunks.size());
        for (var it = staleChunks.longIterator(); it.hasNext(); ) {
            long chunkPos = it.nextLong();
            long foundTime = chunks.get(chunkPos);
            if (foundTime != chunks.defaultReturnValue()) {
                chunksToWrite.put(chunkPos, foundTime);
            }
            it.remove();
        }
        return chunksToWrite;
    }

    public ListenableFuture<?> writeDataToDatabase(Long2LongMap toWrite) {
        try {
            return dbExecutor.submit(() -> database.insertHighlightList(toWrite, dimension));
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Failed to submit db write task for {} disk cache dimension: {}", database.databaseName, dimension.location(), e);
            return Futures.immediateFailedFuture(e);
        }
    }

    // does not remove from local cache
    public ListenableFuture<?> writeStaleHighlightsToDatabase() {
        if (!mc.isSameThread()) {
            throw new RuntimeException("writeStaleHighlightsToDatabase must be called on the main thread");
        }
        var toWrite = collectStaleHighlightsToWrite();
        if (toWrite.isEmpty()) return Futures.immediateVoidFuture();
        return writeDataToDatabase(toWrite);
    }

    @Override
    public void addHighlight(final int x, final int z, final long foundTime) {
        super.addHighlight(x, z, foundTime);
        staleChunks.add(chunkPosToLong(x, z));
    }

    @Override
    public void removeHighlight(final int x, final int z) {
        super.removeHighlight(x, z);
        staleChunks.add(chunkPosToLong(x, z));
        dbExecutor.execute(() -> database.removeHighlight(x, z, dimension));
    }

    @Override
    public CompletableFuture<Long2LongMap> getHighlightsInCustomWindow(final int windowRegionX, final int windowRegionZ, final int windowRegionSize, final ResourceKey<Level> dimension) {
        // stale highlight write is async but our db executor is single threaded so we will always execute after the write task
        return submitTickTask(this::writeStaleHighlightsToDatabase)
            .thenApplyAsync((v) -> {
                int regionXMin = windowRegionX - windowRegionSize;
                int regionZMin = windowRegionZ - windowRegionSize;
                int regionXMax = windowRegionX + windowRegionSize;
                int regionZMax = windowRegionZ + windowRegionSize;
                var resultMap = new Long2LongOpenHashMap();
                ListenableFuture<?> dbLoadFuture = dbExecutor.submit(() -> database.getHighlightsInWindow(
                    dimension,
                    regionXMin, regionXMax,
                    regionZMin, regionZMax,
                    (chunkX, chunkZ, foundTime) -> resultMap.put(chunkPosToLong(chunkX, chunkZ), foundTime)
                ));
                try {
                    dbLoadFuture.get();
                    return resultMap;
                } catch (final Exception e) {
                    XaeroPlus.LOGGER.error("Failed to load highlights in custom window for {} disk cache dimension: {}",
                                           database.databaseName,
                                           this.dimension.location(),
                                           e);
                }
                return Long2LongMaps.EMPTY_MAP;
            });
    }

    @Override
    public void handleWorldChange(final XaeroWorldChangeEvent event) {}

    @Override
    public void handleTick() {}

    @Override
    public void onEnable() {}

    @Override
    public void onDisable() {}

    private final class WindowDataLoadFutureCallback implements FutureCallback<Long2LongMap> {
        @Override
        public void onSuccess(Long2LongMap dataBuf) {
            if (!mc.isSameThread()) {
                XaeroPlus.LOGGER.error("WindowDataLoadFutureCallback must be called on the main thread");
            }
            if (dataBuf.isEmpty()) return;
            // write new data to local cache
            chunks.putAll(dataBuf);
        }

        @Override
        public void onFailure(Throwable t) {
            XaeroPlus.LOGGER.error("Error while moving window for {} disk cache dimension: {}",
                                   database.databaseName,
                                   dimension.location(),
                                   t);
        }
    }
}
