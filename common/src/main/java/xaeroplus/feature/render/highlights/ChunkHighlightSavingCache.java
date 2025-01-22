package xaeroplus.feature.render.highlights;

import com.google.common.util.concurrent.*;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongMaps;
import it.unimi.dsi.fastutil.longs.LongList;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xaero.map.MapProcessor;
import xaero.map.core.XaeroWorldMapCore;
import xaero.map.gui.GuiMap;
import xaeroplus.Globals;
import xaeroplus.XaeroPlus;
import xaeroplus.event.XaeroWorldChangeEvent;
import xaeroplus.util.ChunkUtils;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static net.minecraft.world.level.Level.*;
import static xaeroplus.event.XaeroWorldChangeEvent.WorldChangeType.ENTER_WORLD;
import static xaeroplus.util.ChunkUtils.getActualDimension;
import static xaeroplus.util.GuiMapHelper.*;

public class ChunkHighlightSavingCache implements ChunkHighlightCache, Closeable {
    // these are initialized lazily
    @Nullable private ChunkHighlightDatabase database = null;
    @Nullable private String currentWorldId;
    private final AtomicBoolean cacheReady = new AtomicBoolean(false);
    @Nullable private final String databaseName;
    // Executor used for db read/writes
    @Nullable private ListeningExecutorService workerExecutor;
    // executor used for single threaded tasks that involve changing worlds and preparing the cache for operations
    @NotNull private final ListeningExecutorService parentExecutor;
    private final Map<ResourceKey<Level>, ChunkHighlightCacheDimensionHandler> dimensionCacheMap = new ConcurrentHashMap<>(3);

    public ChunkHighlightSavingCache(final @NotNull String databaseName) {
        this.databaseName = databaseName;
        this.parentExecutor = MoreExecutors.listeningDecorator(
            Executors.newSingleThreadExecutor(
                new ThreadFactoryBuilder()
                    .setNameFormat(databaseName + "-Manager")
                    .setUncaughtExceptionHandler((t, e) -> {
                        XaeroPlus.LOGGER.error("Uncaught exception in {}", t.getName(), e);
                    })
                    .build()));
    }

    @Override
    public boolean addHighlight(final int x, final int z) {
        try {
            ChunkHighlightCacheDimensionHandler cacheForActualDimension = getCacheForActualDimension();
            if (cacheForActualDimension == null) throw new RuntimeException("Didn't find cache for current dimension");
            cacheForActualDimension.addHighlight(x, z);
            return true;
        } catch (final Exception e) {
            XaeroPlus.LOGGER.debug("Error adding highlight to {} disk cache: {}, {}", databaseName, x, z, e);
            return false;
        }
    }

    public void addHighlight(final int x, final int z, final long foundTime, final ResourceKey<Level> dimension) {
        if (dimension == null) return;
        ChunkHighlightCacheDimensionHandler cacheForDimension = getCacheForDimension(dimension, true);
        if (cacheForDimension == null) return;
        cacheForDimension.addHighlight(x, z, foundTime);
    }

    @Override
    public boolean removeHighlight(final int x, final int z) {
        try {
            ChunkHighlightCacheDimensionHandler cacheForActualDimension = getCacheForActualDimension();
            if (cacheForActualDimension == null) throw new RuntimeException("Didn't find cache for current dimension");
            cacheForActualDimension.removeHighlight(x, z);
            return true;
        } catch (final Exception e) {
            XaeroPlus.LOGGER.debug("Error removing highlight from {} disk cache: {}, {}", databaseName, x, z, e);
            return false;
        }
    }

    @Override
    public boolean isHighlighted(final int chunkPosX, final int chunkPosZ, final ResourceKey<Level> dimensionId) {
        if (dimensionId == null) return false;
        ChunkHighlightCacheDimensionHandler cacheForDimension = getCacheForDimension(dimensionId, false);
        if (cacheForDimension == null) return false;
        return cacheForDimension.isHighlighted(chunkPosX, chunkPosZ, dimensionId);
    }

    public boolean isHighlighted(final int chunkPosX, final int chunkPosZ) {
        ChunkHighlightCacheDimensionHandler cacheForDimension = getCacheForDimension(getActualDimension(), false);
        if (cacheForDimension == null) return false;
        return cacheForDimension.isHighlighted(chunkPosX, chunkPosZ, getActualDimension());
    }

    @Override
    public LongList getHighlightsSnapshot(final ResourceKey<Level> dimensionId) {
        if (dimensionId == null) return LongList.of();
        ChunkHighlightCacheDimensionHandler cacheForDimension = getCacheForDimension(dimensionId, false);
        if (cacheForDimension == null) return LongList.of();
        return cacheForDimension.getHighlightsSnapshot(dimensionId);
    }

    /**
     * Gets all highlight data both from the database and local cache in a square set of regions.
     * Can be used to get highlight data that may be outside the current loaded window
     *
     * @param dimension the dimension to get highlights for
     * @param windowRegionX Centered region X coordinate
     * @param windowRegionZ Centered region Z coordinate
     * @param windowRegionSize Region window size.
     *                   Total area of square = (2 * (windowRegionSize + 1)) ^ 2
     *                   region = 1024 (32x32) chunks
     * @return Listenable future of a Long2LongMap: packed chunk coordinates -> found time unix epoch
     *       To convert packed coordinates in the map, see `ChunkUtils.longToChunkX` and `ChunkUtils.longToChunkZ`
     */
    public ListenableFuture<Long2LongMap> getHighlightsInCustomWindow(int windowRegionX, int windowRegionZ, int windowRegionSize, ResourceKey<Level> dimension) {
        ChunkHighlightCacheDimensionHandler cacheForDimension = getCacheForDimension(dimension, false);
        if (cacheForDimension == null) return Futures.immediateFuture(Long2LongMaps.EMPTY_MAP);
        return cacheForDimension.getHighlightsInCustomWindow(windowRegionX, windowRegionZ, windowRegionSize);
    }

    @Override
    public void handleWorldChange(final XaeroWorldChangeEvent event) {
        parentExecutor.execute(() -> {
            switch (event.worldChangeType()) {
                case ENTER_WORLD -> {
                    cacheReady.set(false);
                    reset();
                    if (initializeWorld()) {
                        cacheReady.set(true);
                    }
                }
                case EXIT_WORLD -> {
                    // make sure we mark as unready to prevent further mutations
                    if (cacheReady.compareAndSet(true, false)) {
                        try {
                            Futures.allAsList(flushAllChunks()).get(30, TimeUnit.SECONDS);
                        } catch (final Exception e) {
                            XaeroPlus.LOGGER.error("Error saving all chunks before world change", e);
                        }
                    }
                    reset();
                }
                case VIEWED_DIMENSION_SWITCH -> {
                    loadChunksInCurrentDimension();
                }
                case ACTUAL_DIMENSION_SWITCH -> {
                    loadChunksInActualDimension();
                }
            }
        });
    }

    private synchronized void reset() {
        this.currentWorldId = null;
        if (this.workerExecutor != null) {
            this.workerExecutor.shutdown();
            try {
                this.workerExecutor.awaitTermination(3L, TimeUnit.SECONDS);
            } catch (final Throwable e) {
                XaeroPlus.LOGGER.error("Timed out waiting for {} executor to shutdown", databaseName, e);
            }
        }
        if (this.database != null) this.database.close();
        // dimension cache instances will be GC'd, no need to explicitly clear them
        this.dimensionCacheMap.clear();
        this.database = null;
    }

    // note: writes occur on the worker thread
    private List<ListenableFuture<?>> flushAllChunks() {
        return getAllCaches().stream()
            .map(ChunkHighlightCacheDimensionHandler::writeAllHighlightsToDatabase)
            .collect(Collectors.toList());
    }

    public ChunkHighlightCacheDimensionHandler getCacheForActualDimension() {
        if (!cacheReady.get()) return null;
        return getCacheForDimension(ChunkUtils.getActualDimension(), true);
    }

    private ChunkHighlightCacheDimensionHandler initializeDimensionCacheHandler(final ResourceKey<Level> dimension) {
        if (dimension == null) return null;
        var db = this.database;
        var executor = this.workerExecutor;
        if (db == null || executor == null) {
            XaeroPlus.LOGGER.error("[{}] Unable to initialize {} disk cache handler for: {}, database: {} or executor: {} is null", Thread.currentThread().getName(), databaseName, dimension.location(), db, executor);
            return null;
        }
        var cacheHandler = new ChunkHighlightCacheDimensionHandler(dimension, db, executor);
        db.initializeDimension(dimension);
        this.dimensionCacheMap.put(dimension, cacheHandler);
        return cacheHandler;
    }

    public ChunkHighlightCacheDimensionHandler getCacheForDimension(final ResourceKey<Level> dimension, boolean create) {
        if (!cacheReady.get()) return null;
        if (dimension == null) return null;
        var dimensionCache = dimensionCacheMap.get(dimension);
        if (dimensionCache == null) {
            if (!create) return null;
            XaeroPlus.LOGGER.info("Initializing {} disk cache for dimension: {}", databaseName, dimension.location());
            dimensionCache = initializeDimensionCacheHandler(dimension);
        }
        return dimensionCache;
    }

    public List<ChunkHighlightCacheDimensionHandler> getAllCaches() {
        return List.copyOf(dimensionCacheMap.values());
    }

    public List<ChunkHighlightCacheDimensionHandler> getCachesExceptDimension(final ResourceKey<Level> dimension) {
        var caches = new ArrayList<ChunkHighlightCacheDimensionHandler>(dimensionCacheMap.size());
        for (var entry : dimensionCacheMap.entrySet()) {
            if (!entry.getKey().equals(dimension)) {
                caches.add(entry.getValue());
            }
        }
        return caches;
    }

    public List<ChunkHighlightCacheDimensionHandler> getCachesExceptDimensions(final List<ResourceKey<Level>> dimensions) {
        var caches = new ArrayList<ChunkHighlightCacheDimensionHandler>(dimensionCacheMap.size());
        for (var entry : dimensionCacheMap.entrySet()) {
            if (!dimensions.contains(entry.getKey())) {
                caches.add(entry.getValue());
            }
        }
        return caches;
    }

    // returns false if we were not able to get to a ready state
    // will happen if we are disconnecting from a server where the mc world is not loaded
    private synchronized boolean initializeWorld() {
        try {
            MapProcessor mapProcessor = XaeroWorldMapCore.currentSession.getMapProcessor();
            if (mapProcessor == null) return false;
            final String worldId = mapProcessor.getCurrentWorldId();
            if (worldId == null) return false;
            this.currentWorldId = worldId;
            this.workerExecutor = MoreExecutors.listeningDecorator(
                Executors.newSingleThreadExecutor(
                    new ThreadFactoryBuilder()
                        .setNameFormat(databaseName + "-Worker")
                        .setUncaughtExceptionHandler((t, e) -> {
                            XaeroPlus.LOGGER.error("Uncaught exception handler in {}", t.getName(), e);
                        })
                        .build()));
            this.database = new ChunkHighlightDatabase(worldId, databaseName);
            initializeDimensionCacheHandler(OVERWORLD);
            initializeDimensionCacheHandler(NETHER);
            initializeDimensionCacheHandler(END);
            loadChunksInCurrentDimension();
            return true;
        } catch (final Exception e) {
            // expected on game launch
            reset(); // ensure we don't leave ourselves in a half init state somehow
            return false;
        }
    }

    private void loadChunksInActualDimension() {
        var cacheForActualDimension = getCacheForActualDimension();
        if (cacheForActualDimension == null) return;
        cacheForActualDimension
            .setWindow(ChunkUtils.actualPlayerRegionX(), ChunkUtils.actualPlayerRegionZ(), getMinimapRegionWindowSize());
    }

    private void loadChunksInCurrentDimension() {
        var cacheForCurrentDimension = getCacheForActualDimension();
        if (cacheForCurrentDimension == null) return;
        cacheForCurrentDimension
            .setWindow(ChunkUtils.getPlayerRegionX(), ChunkUtils.getPlayerRegionZ(), getMinimapRegionWindowSize());
    }

    @Override
    public void onEnable() {
        handleWorldChange(new XaeroWorldChangeEvent(ENTER_WORLD, null, ChunkUtils.getActualDimension()));
    }

    @Override
    public void onDisable() {
        parentExecutor.execute(() -> {
            cacheReady.set(false);
            try {
                Futures.allAsList(flushAllChunks()).get(30, TimeUnit.SECONDS);
            } catch (final Exception e) {
                XaeroPlus.LOGGER.error("Error saving all chunks before disabling", e);
            }
            reset();
        });
    }

    @Override
    public Long2LongMap getHighlightsState() {
        return null;
    }

    @Override
    public void loadPreviousState(final Long2LongMap state) {

    }

    public int getMinimapRegionWindowSize() {
        return Math.max(3, Globals.minimapScaleMultiplier);
    }

    int tickCounter = 0;

    @Override
    public void handleTick() {
        if (!cacheReady.get()) return;
        if (XaeroWorldMapCore.currentSession == null) return;
        // limit so we don't overflow
        if (tickCounter > 2400) tickCounter = 0;
        if (tickCounter++ % 30 != 0) { // run once every 1.5 seconds
            return;
        }
        // autosave current window every 60 seconds
        if (tickCounter % 1200 == 0) {
            getAllCaches().forEach(ChunkHighlightCacheDimensionHandler::writeAllHighlightsToDatabase);
            return;
        }

        final ResourceKey<Level> mapDimension = Globals.getCurrentDimensionId();
        final ResourceKey<Level> actualDimension = ChunkUtils.getActualDimension();

        final int windowSize;
        final int actualPlayerRegionX = ChunkUtils.actualPlayerRegionX();
        final int actualPlayerRegionZ = ChunkUtils.actualPlayerRegionZ();
        final int windowCenterX;
        final int windowCenterZ;

        Optional<GuiMap> guiMapOptional = getGuiMap();
        if (guiMapOptional.isPresent()) {
            var guiMap = guiMapOptional.get();
            windowSize = getGuiMapRegionSize(guiMap);
            windowCenterX = getGuiMapCenterRegionX(guiMap);
            windowCenterZ = getGuiMapCenterRegionZ(guiMap);
        } else {
            windowSize = getMinimapRegionWindowSize();
            windowCenterX = ChunkUtils.getPlayerRegionX();
            windowCenterZ = ChunkUtils.getPlayerRegionZ();
        }
        var cacheForDimension = getCacheForDimension(mapDimension, true);
        if (cacheForDimension != null) cacheForDimension.setWindow(windowCenterX, windowCenterZ, windowSize);
        if (mapDimension == actualDimension) {
            getCachesExceptDimension(mapDimension)
                .forEach(cache -> cache.setWindow(0, 0, 0));
        } else {
            var actualDimCache = getCacheForDimension(actualDimension, true);
            if (actualDimCache != null) {
                actualDimCache.setWindow(actualPlayerRegionX, actualPlayerRegionZ, windowSize);
            }
            getCachesExceptDimensions(List.of(mapDimension, actualDimension))
                .forEach(cache -> cache.setWindow(0, 0, 0));
        }
    }

    @Override
    public void close() throws IOException {
        // does not await the shutdown
        // this saving cache instance should never be reused after this is called
        parentExecutor.shutdown();
    }
}
