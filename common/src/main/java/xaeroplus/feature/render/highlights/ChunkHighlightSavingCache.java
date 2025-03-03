package xaeroplus.feature.render.highlights;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongMaps;
import net.minecraft.client.Minecraft;
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
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static net.minecraft.world.level.Level.*;
import static xaeroplus.event.XaeroWorldChangeEvent.WorldChangeType.ENTER_WORLD;
import static xaeroplus.util.GuiMapHelper.*;

public class ChunkHighlightSavingCache implements ChunkHighlightCache, Closeable {
    // these are initialized lazily
    @Nullable private ChunkHighlightDatabase database = null;
    @Nullable private String currentWorldId;
    private final AtomicBoolean cacheReady = new AtomicBoolean(false);
    @Nullable private final String databaseName;
    // Executor used for db read/writes
    @Nullable private ListeningExecutorService dbExecutor;
    // executor used for single threaded tasks that involve changing worlds and preparing the cache for operations
    @NotNull private final ListeningExecutorService parentExecutor;
    private final Map<ResourceKey<Level>, ChunkHighlightCacheDimensionHandler> dimensionCacheMap = new ConcurrentHashMap<>(3);
    private final Queue<Runnable> initializeTaskQueue = new ConcurrentLinkedQueue<>();
    Minecraft mc = Minecraft.getInstance();

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
    public void addHighlight(final int x, final int z) {
        try {
            ChunkHighlightCacheDimensionHandler cacheForActualDimension = getCacheForActualDimension();
            if (cacheForActualDimension == null) {
                // if the cache is not ready yet, queue the highlight to be added
                initializeTaskQueue.add(() -> addHighlight(x, z));
                return;
            }
            cacheForActualDimension.addHighlight(x, z);
        } catch (final Exception e) {
            XaeroPlus.LOGGER.warn("Error adding highlight to {} disk cache: {}, {}", databaseName, x, z, e);
        }
    }

    @Override
    public void removeHighlight(final int x, final int z) {
        try {
            ChunkHighlightCacheDimensionHandler cacheForActualDimension = getCacheForActualDimension();
            if (cacheForActualDimension == null) {
                // if the cache is not ready yet, queue the highlight to be removed
                initializeTaskQueue.add(() -> removeHighlight(x, z));
                return;
            }
            cacheForActualDimension.removeHighlight(x, z);
        } catch (final Exception e) {
            XaeroPlus.LOGGER.warn("Error removing highlight from {} disk cache: {}, {}", databaseName, x, z, e);
        }
    }

    @Override
    public boolean isHighlighted(final int chunkPosX, final int chunkPosZ, final ResourceKey<Level> dimensionId) {
        if (dimensionId == null) return false;
        ChunkHighlightCacheDimensionHandler cacheForDimension = getCacheForDimension(dimensionId, false);
        if (cacheForDimension == null) return false;
        return cacheForDimension.isHighlighted(chunkPosX, chunkPosZ, dimensionId);
    }

    @Override
    public Long2LongMap getCacheMap(final ResourceKey<Level> dimensionId) {
        if (dimensionId == null) return Long2LongMaps.EMPTY_MAP;
        ChunkHighlightCacheDimensionHandler cacheForDimension = getCacheForDimension(dimensionId, false);
        if (cacheForDimension == null) return Long2LongMaps.EMPTY_MAP;
        return cacheForDimension.getCacheMap(dimensionId);
    }

    @Override
    public CompletableFuture<Long2LongMap> getHighlightsInCustomWindow(final int windowRegionX, final int windowRegionZ, final int windowRegionSize, final ResourceKey<Level> dimension) {
        if (dimension == null) return CompletableFuture.completedFuture(Long2LongMaps.EMPTY_MAP);
        ChunkHighlightCacheDimensionHandler cacheForDimension = getCacheForDimension(dimension, true);
        if (cacheForDimension == null) return CompletableFuture.completedFuture(Long2LongMaps.EMPTY_MAP);
        return cacheForDimension.getHighlightsInCustomWindow(windowRegionX,
                                                             windowRegionZ, windowRegionSize, dimension);
    }

    @Override
    public void handleWorldChange(final XaeroWorldChangeEvent event) {
        parentExecutor.execute(() -> {
            switch (event.worldChangeType()) {
                case ENTER_WORLD -> {
                    if (!cacheReady.get()) {
                        if (initializeWorld()) {
                            cacheReady.set(true);
                        }
                    } else {
                        XaeroPlus.LOGGER.warn("[{}] Entered world when cache was already initialized", databaseName);
                    }
                }
                case EXIT_WORLD -> {
                    // make sure we mark as unready to prevent further mutations
                    if (cacheReady.compareAndSet(true, false)) {
                        try {
                            CompletableFuture.allOf(flushAllChunks().toArray(new CompletableFuture[0])).get(30, TimeUnit.SECONDS);
                        } catch (final Exception e) {
                            XaeroPlus.LOGGER.error("Error saving all chunks before world change", e);
                        }
                    } else {
                        XaeroPlus.LOGGER.warn("[{}] Exited world when cache was already uninitialized", databaseName);
                    }
                    reset();
                }
                case VIEWED_DIMENSION_SWITCH -> {
                    submitTickTask(this::loadChunksInViewedDimension);
                }
                case ACTUAL_DIMENSION_SWITCH -> {
                    submitTickTask(this::loadChunksOnActualDimensionSwitch);
                }
            }
        });
    }

    private synchronized void reset() {
        this.currentWorldId = null;
        if (this.dbExecutor != null) {
            var closeFuture = this.dbExecutor.submit(() -> {
                if (this.database != null) {
                    this.database.close();
                }
            });
            try {
                this.dbExecutor.shutdown();
                closeFuture.get(3L, TimeUnit.SECONDS);
                this.dbExecutor.awaitTermination(3L, TimeUnit.SECONDS);
            } catch (final Throwable e) {
                XaeroPlus.LOGGER.error("Timed out waiting for {} executor to shutdown", databaseName, e);
            }
        }
        if (this.database != null) this.database.close();
        // dimension cache instances will be GC'd, no need to explicitly clear them
        this.dimensionCacheMap.clear();
        this.database = null;
        this.initializeTaskQueue.clear();
    }

    // note: writes occur on the worker thread
    private List<CompletableFuture<?>> flushAllChunks() {
        return getAllCaches().stream()
            .map(cache -> submitTickTask(cache::writeStaleHighlightsToDatabase))
            .collect(Collectors.toList());
    }

    public ChunkHighlightCacheDimensionHandler getCacheForActualDimension() {
        if (!cacheReady.get()) return null;
        return getCacheForDimension(ChunkUtils.getActualDimension(), true);
    }

    private ChunkHighlightCacheDimensionHandler initializeDimensionCacheHandler(final ResourceKey<Level> dimension) {
        if (dimension == null) return null;
        var db = this.database;
        var executor = this.dbExecutor;
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
            this.dbExecutor = MoreExecutors.listeningDecorator(
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
            loadChunksInViewedDimension();
            if (!initializeTaskQueue.isEmpty()) XaeroPlus.LOGGER.info("[{}] Running {} queued tasks", databaseName, initializeTaskQueue.size());
            while (!this.initializeTaskQueue.isEmpty()) {
                submitTickTask(this.initializeTaskQueue.poll());
            }
            return true;
        } catch (final Exception e) {
            // expected on game launch
            reset(); // ensure we don't leave ourselves in a half init state somehow
            return false;
        }
    }

    private void loadChunksOnActualDimensionSwitch() {
        var cacheForActualDimension = getCacheForActualDimension();
        if (cacheForActualDimension == null) return;
        cacheForActualDimension
            .setWindow(ChunkUtils.actualPlayerRegionX(), ChunkUtils.actualPlayerRegionZ(), getMinimapRegionWindowSize());
    }

    private void loadChunksInViewedDimension() {
        var viewedDim = Globals.getCurrentDimensionId();
        var cacheForCurrentDimension = getCacheForDimension(viewedDim, true);
        if (cacheForCurrentDimension == null) return;
        final int windowSize;
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
        cacheForCurrentDimension
            .setWindow(windowCenterX, windowCenterZ, windowSize);
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
                CompletableFuture.allOf(flushAllChunks().toArray(new CompletableFuture[0])).get(30, TimeUnit.SECONDS);
            } catch (final Exception e) {
                XaeroPlus.LOGGER.error("Error saving all chunks before disabling", e);
            }
            reset();
        });
    }

    public int getMinimapRegionWindowSize() {
        return Math.max(3, Globals.minimapScaleMultiplier);
    }

    int tickCounter = 0;

    @Override
    public void handleTick() {
        if (!cacheReady.get()) return;
        if (XaeroWorldMapCore.currentSession == null) return;
        // reduce likelihood of all caches updating at the same time
        // changing the window involves iterating through every chunk in the cache to find which are now outside the window
        // which can be expensive if there are thousands of cache entries
        // this does make the update interval setting kind of a lie, but its for the best
        int jitter = ThreadLocalRandom.current().nextInt(0, 10);
        // only update window on an interval
        if (++tickCounter < 10 + jitter) {
            return;
        }
        tickCounter = 0;

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
