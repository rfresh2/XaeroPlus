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
        for (var it = Long2LongMaps.fastIterator(chunks); it.hasNext(); ) {
            var entry = it.next();
            final long chunkPos = entry.getLongKey();
            final int chunkX = ChunkUtils.longToChunkX(chunkPos);
            final int chunkZ = ChunkUtils.longToChunkZ(chunkPos);
            if (chunkX < chunkXMin
                || chunkX > chunkXMax
                || chunkZ < chunkZMin
                || chunkZ > chunkZMax) {
                it.remove();
                if (staleChunks.contains(chunkPos)) {
                    dataBuf.put(chunkPos, entry.getLongValue());
                }
            }
        }
        return dbExecutor.submit(() -> database.insertHighlightList(dataBuf, dimension));
    }

        // does not remove from local cache
    public ListenableFuture<?> writeStaleHighlightsToDatabase() {
        if (!mc.isSameThread()) {
            throw new RuntimeException("writeStaleHighlightsToDatabase must be called on the main thread");
        }
        if (staleChunks.isEmpty()) return Futures.immediateVoidFuture();
        Long2LongMap chunksToWrite = new Long2LongOpenHashMap(staleChunks.size());
        try {
            for (var it = staleChunks.longIterator(); it.hasNext(); ) {
                long chunkPos = it.nextLong();
                long foundTime = chunks.get(chunkPos);
                if (foundTime != chunks.defaultReturnValue()) {
                    chunksToWrite.put(chunkPos, foundTime);
                }
                it.remove();
            }
            return dbExecutor.submit(() -> database.insertHighlightList(chunksToWrite, dimension));
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error while writing all highlights to {} disk cache dimension: {}", database.databaseName, dimension.location(), e);
        }
        return Futures.immediateVoidFuture();
    }

    @Override
    public void removeHighlight(final int x, final int z) {
        super.removeHighlight(x, z);
        dbExecutor.execute(() -> database.removeHighlight(x, z, dimension));
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
