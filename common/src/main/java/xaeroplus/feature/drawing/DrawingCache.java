package xaeroplus.feature.drawing;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongMaps;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaero.map.MapProcessor;
import xaero.map.core.XaeroWorldMapCore;
import xaero.map.gui.GuiMap;
import xaeroplus.Globals;
import xaeroplus.XaeroPlus;
import xaeroplus.event.XaeroWorldChangeEvent;
import xaeroplus.feature.db.DrawingDatabase;
import xaeroplus.feature.render.line.Line;
import xaeroplus.module.ModuleManager;
import xaeroplus.module.impl.TickTaskExecutor;
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

public class DrawingCache implements Closeable {
    private DrawingDatabase database;
    private String currentWorldId;
    private final AtomicBoolean cacheReady = new AtomicBoolean(false);
    private final String databaseName;
    private ListeningExecutorService dbExecutor;
    private final ListeningExecutorService parentExecutor;
    private final Map<ResourceKey<Level>, DrawingHighlightCacheDimensionHandler> dimensionCacheMap = new ConcurrentHashMap<>(3);
    private final Map<ResourceKey<Level>, DrawingLinesCacheDimensionHandler> linesCacheMap = new ConcurrentHashMap<>(3);
    private final Queue<Runnable> initializeTaskQueue = new ConcurrentLinkedQueue<>();
    Minecraft mc = Minecraft.getInstance();

    public DrawingCache(final String databaseName) {
        this.databaseName = databaseName;
        this.parentExecutor = MoreExecutors.listeningDecorator(
            Executors.newSingleThreadExecutor(
                new ThreadFactoryBuilder()
                    .setNameFormat(databaseName + "-Manager")
                    .setUncaughtExceptionHandler((t, e) -> {
                        XaeroPlus.LOGGER.error("Uncaught exception in {}", t.getName(), e);
                    })
                    .build()
            )
        );
    }

    public void addHighlight(final int x, final int z, final int color, final ResourceKey<Level> dimension) {
        try {
            DrawingHighlightCacheDimensionHandler cacheForActualDimension = getCacheForDimension(dimension, true);
            if (cacheForActualDimension == null) {
                // if the cache is not ready yet, queue the highlight to be added
                initializeTaskQueue.add(() -> addHighlight(x, z, color, dimension));
                return;
            }
            cacheForActualDimension.addHighlight(x, z, color);
        } catch (final Exception e) {
            XaeroPlus.LOGGER.warn("Error adding highlight to {} disk cache: {}, {}", databaseName, x, z, e);
        }
    }

    public void addLine(Line line, int color, ResourceKey<Level> dimension) {
        try {
            DrawingLinesCacheDimensionHandler cacheForActualDimension = getLinesCacheForDimension(dimension, true);
            if (cacheForActualDimension == null) {
                // if the cache is not ready yet, queue the line to be added
                initializeTaskQueue.add(() -> addLine(line, color, dimension));
                return;
            }
            cacheForActualDimension.addLine(line, color);
        } catch (final Exception e) {
            XaeroPlus.LOGGER.warn("Error adding line to {} disk cache: {}, {}", databaseName, line, e);
        }
    }

    public void removeHighlight(final int x, final int z, final ResourceKey<Level> dimension) {
        try {
            DrawingHighlightCacheDimensionHandler cacheForActualDimension = getCacheForDimension(dimension, true);
            if (cacheForActualDimension == null) {
                // if the cache is not ready yet, queue the highlight to be removed
                initializeTaskQueue.add(() -> removeHighlight(x, z, dimension));
                return;
            }
            cacheForActualDimension.removeHighlight(x, z);
        } catch (final Exception e) {
            XaeroPlus.LOGGER.warn("Error removing highlight from {} disk cache: {}, {}", databaseName, x, z, e);
        }
    }

    public void removeLine(Line line, ResourceKey<Level> dimension) {
        try {
            DrawingLinesCacheDimensionHandler cacheForActualDimension = getLinesCacheForDimension(dimension, true);
            if (cacheForActualDimension == null) {
                // if the cache is not ready yet, queue the line to be removed
                initializeTaskQueue.add(() -> removeLine(line, dimension));
                return;
            }
            cacheForActualDimension.removeLine(line);
        } catch (final Exception e) {
            XaeroPlus.LOGGER.warn("Error removing line from {} disk cache: {}, {}", databaseName, line, e);
        }
    }

    public Long2LongMap getCacheMap(final ResourceKey<Level> dimensionId) {
        if (dimensionId == null) return Long2LongMaps.EMPTY_MAP;
        DrawingHighlightCacheDimensionHandler cacheForDimension = getCacheForDimension(dimensionId, false);
        if (cacheForDimension == null) return Long2LongMaps.EMPTY_MAP;
        return cacheForDimension.getCacheMap(dimensionId);
    }

    public Object2IntMap<Line> getLines(final ResourceKey<Level> dimension) {
        if (dimension == null) return Object2IntMaps.emptyMap();
        var cacheForDimension = getLinesCacheForDimension(dimension, false);
        if (cacheForDimension == null) return Object2IntMaps.emptyMap();
        return cacheForDimension.getLines();
    }

    public void handleWorldChange(final XaeroWorldChangeEvent event) {
        parentExecutor.execute(() -> {
            switch (event.worldChangeType()) {
                case ENTER_WORLD -> {
                    if (!cacheReady.get()) {
                        if (initializeWorld()) {
                            cacheReady.set(true);
                            submitTickTask(() -> {
                                loadChunksInViewedDimension();
                                loadLinesInViewedDimension();
                            });
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
                            CompletableFuture.allOf(flushAllLines().toArray(new CompletableFuture[0])).get(30, TimeUnit.SECONDS);
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
                    submitTickTask(this::loadLinesInViewedDimension);
                }
                case ACTUAL_DIMENSION_SWITCH -> {
                    submitTickTask(this::loadChunksOnActualDimensionSwitch);
                    submitTickTask(this::loadLinesOnActualDimensionSwitch);
                }
            }
        });
    }

    private CompletableFuture<?> submitTickTask(final Runnable runnable) {
        return ModuleManager.getModule(TickTaskExecutor.class).submit(runnable);
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
        this.linesCacheMap.clear();
        this.database = null;
        this.initializeTaskQueue.clear();
    }

    // note: writes occur on the worker thread
    private List<CompletableFuture<?>> flushAllChunks() {
        return getAllCaches().stream()
            .map(cache -> submitTickTask(cache::writeStaleHighlightsToDatabase))
            .collect(Collectors.toList());
    }

    private List<CompletableFuture<?>> flushAllLines() {
        return getAllLinesCaches().stream()
            .map(cache -> submitTickTask(cache::writeStaleLinesToDatabase))
            .collect(Collectors.toList());
    }

    public DrawingHighlightCacheDimensionHandler getCacheForActualDimension() {
        if (!cacheReady.get()) return null;
        return getCacheForDimension(ChunkUtils.getActualDimension(), true);
    }

    private DrawingHighlightCacheDimensionHandler initializeDimensionCacheHandler(final ResourceKey<Level> dimension) {
        if (dimension == null) return null;
        var db = this.database;
        var executor = this.dbExecutor;
        if (db == null || executor == null) {
            XaeroPlus.LOGGER.error("[{}] Unable to initialize {} disk cache handler for: {}, database: {} or executor: {} is null", Thread.currentThread().getName(), databaseName, dimension.location(), db, executor);
            return null;
        }
        var cacheHandler = new DrawingHighlightCacheDimensionHandler(dimension, db, executor);
        db.initializeDimension(dimension);
        this.dimensionCacheMap.put(dimension, cacheHandler);
        return cacheHandler;
    }

    private DrawingLinesCacheDimensionHandler initializeLinesCacheHandler(final ResourceKey<Level> dimension) {
        if (dimension == null) return null;
        var db = this.database;
        var executor = this.dbExecutor;
        if (db == null || executor == null) {
            XaeroPlus.LOGGER.error("[{}] Unable to initialize {} disk lines cache handler for: {}, database: {} or executor: {} is null", Thread.currentThread().getName(), databaseName, dimension.location(), db, executor);
            return null;
        }
        var linesCacheHandler = new DrawingLinesCacheDimensionHandler(dimension, db, executor);
        db.initializeDimension(dimension);
        this.linesCacheMap.put(dimension, linesCacheHandler);
        return linesCacheHandler;
    }

    public DrawingHighlightCacheDimensionHandler getCacheForDimension(final ResourceKey<Level> dimension, boolean create) {
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

    public DrawingLinesCacheDimensionHandler getLinesCacheForDimension(final ResourceKey<Level> dimension, boolean create) {
        if (!cacheReady.get()) return null;
        if (dimension == null) return null;
        var linesCache = linesCacheMap.get(dimension);
        if (linesCache == null) {
            if (!create) return null;
            XaeroPlus.LOGGER.info("Initializing {} disk lines cache for dimension: {}", databaseName, dimension.location());
            linesCache = initializeLinesCacheHandler(dimension);
        }
        return linesCache;
    }

    public List<DrawingHighlightCacheDimensionHandler> getAllCaches() {
        return List.copyOf(dimensionCacheMap.values());
    }

    public List<DrawingLinesCacheDimensionHandler> getAllLinesCaches() {
        return List.copyOf(linesCacheMap.values());
    }

    public List<DrawingHighlightCacheDimensionHandler> getCachesExceptDimension(final ResourceKey<Level> dimension) {
        var caches = new ArrayList<DrawingHighlightCacheDimensionHandler>(dimensionCacheMap.size());
        for (var entry : dimensionCacheMap.entrySet()) {
            if (!entry.getKey().equals(dimension)) {
                caches.add(entry.getValue());
            }
        }
        return caches;
    }

    public List<DrawingHighlightCacheDimensionHandler> getCachesExceptDimensions(final List<ResourceKey<Level>> dimensions) {
        var caches = new ArrayList<DrawingHighlightCacheDimensionHandler>(dimensionCacheMap.size());
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
            this.database = new DrawingDatabase(worldId, databaseName);
            initializeDimensionCacheHandler(OVERWORLD);
            initializeDimensionCacheHandler(NETHER);
            initializeDimensionCacheHandler(END);
            initializeLinesCacheHandler(OVERWORLD);
            initializeLinesCacheHandler(NETHER);
            initializeLinesCacheHandler(END);
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

    private void loadLinesOnActualDimensionSwitch() {
        var linesCacheForActualDimension = getLinesCacheForDimension(ChunkUtils.getActualDimension(), true);
        if (linesCacheForActualDimension == null) return;
        linesCacheForActualDimension.loadLines();
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

    private void loadLinesInViewedDimension() {
        var viewedDim = Globals.getCurrentDimensionId();
        var linesCacheForCurrentDimension = getLinesCacheForDimension(viewedDim, true);
        if (linesCacheForCurrentDimension == null) return;
        linesCacheForCurrentDimension.loadLines();
    }

    public void onEnable() {
        handleWorldChange(new XaeroWorldChangeEvent(ENTER_WORLD, null, ChunkUtils.getActualDimension()));
    }

    public void onDisable() {
        parentExecutor.execute(() -> {
            cacheReady.set(false);
            try {
                CompletableFuture.allOf(flushAllChunks().toArray(new CompletableFuture[0])).get(30, TimeUnit.SECONDS);
                CompletableFuture.allOf(flushAllLines().toArray(new CompletableFuture[0])).get(30, TimeUnit.SECONDS);
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
