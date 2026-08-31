package xaeroplus.feature.highlights;

import com.google.common.util.concurrent.*;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongMaps;
import it.unimi.dsi.fastutil.longs.LongCollection;
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
import xaeroplus.util.Wait;
import xaeroplus.util.timer.Timer;
import xaeroplus.util.timer.Timers;

import java.io.Closeable;
import java.io.IOException;
import java.time.Instant;
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
    @Nullable private final String name;
    // Executor used for db read/writes
    @Nullable private ListeningExecutorService dbExecutor;
    // executor used for single threaded tasks that involve changing worlds and preparing the cache for operations
    @NotNull private final ListeningExecutorService parentExecutor;
    private final Map<ResourceKey<Level>, ChunkHighlightCacheDimensionHandler> dimensionCacheMap = new ConcurrentHashMap<>(3);
    // highlight add/remove ops queued while the cache is initializing
    private final Queue<QueuedInitOperation> initOperationQueue = new ConcurrentLinkedQueue<>();
    Minecraft mc = Minecraft.getInstance();

    public ChunkHighlightSavingCache(final @NotNull String name) {
        this.name = name;
        this.parentExecutor = MoreExecutors.listeningDecorator(
            Executors.newSingleThreadExecutor(
                new ThreadFactoryBuilder()
                    .setNameFormat(name + "-Manager")
                    .setUncaughtExceptionHandler((t, e) -> {
                        XaeroPlus.LOGGER.error("Uncaught exception in {}", t.getName(), e);
                    })
                    .build()));
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public void addHighlight(final int x, final int z) {
        addHighlight(x, z, ChunkUtils.getActualDimension());
    }

    @Override
    public void addHighlight(final int x, final int z, final ResourceKey<Level> dimension) {
        try {
            ChunkHighlightCacheDimensionHandler cacheForActualDimension = getCacheForDimension(dimension, true);
            if (cacheForActualDimension == null) {
                // if the cache is not ready yet, queue the highlight to be added
                addInitOperation(() -> addHighlight(x, z, dimension));
                return;
            }
            cacheForActualDimension.addHighlight(x, z);
        } catch (final Exception e) {
            XaeroPlus.LOGGER.warn("Error adding highlight to {} disk cache: {}, {}", name, x, z, e);
        }
    }

    @Override
    public void addHighlight(final int x, final int z, final long foundTime) {
        addHighlight(x, z, foundTime, ChunkUtils.getActualDimension());
    }

    @Override
    public void addHighlight(final int x, final int z, final long foundTime, final ResourceKey<Level> dimension) {
        try {
            ChunkHighlightCacheDimensionHandler cacheForActualDimension = getCacheForDimension(dimension, true);
            if (cacheForActualDimension == null) {
                // if the cache is not ready yet, queue the highlight to be added
                addInitOperation(() -> addHighlight(x, z, foundTime, dimension));
                return;
            }
            cacheForActualDimension.addHighlight(x, z, foundTime);
        } catch (final Exception e) {
            XaeroPlus.LOGGER.warn("Error adding highlight to {} disk cache: {}, {}", name, x, z, e);
        }
    }

    @Override
    public void removeHighlight(final int x, final int z) {
        removeHighlight(x, z, ChunkUtils.getActualDimension());
    }

    @Override
    public void removeHighlight(final int x, final int z, final ResourceKey<Level> dimension) {
        try {
            ChunkHighlightCacheDimensionHandler cacheForActualDimension = getCacheForDimension(dimension, true);
            if (cacheForActualDimension == null) {
                // if the cache is not ready yet, queue the highlight to be removed
                initOperationQueue.add(new QueuedInitOperation(() -> removeHighlight(x, z, dimension)));
                return;
            }
            cacheForActualDimension.removeHighlight(x, z);
        } catch (final Exception e) {
            XaeroPlus.LOGGER.warn("Error removing highlight from {} disk cache: {}, {}", name, x, z, e);
        }
    }

    @Override
    public void removeHighlights(final LongCollection toRemove) {
        removeHighlights(toRemove, ChunkUtils.getActualDimension());
    }

    @Override
    public void removeHighlights(final LongCollection toRemove, final ResourceKey<Level> dimension) {
        try {
            var cacheForActualDimension = getCacheForDimension(dimension, true);
            if (cacheForActualDimension == null) {
                addInitOperation(() -> removeHighlights(toRemove, dimension));
                return;
            }
            cacheForActualDimension.removeHighlights(toRemove);
        } catch (final Exception e) {
            XaeroPlus.LOGGER.warn("Error removing highlights from {} disk cache: {}, {}", name, toRemove, dimension, e);
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
        return cacheForDimension.getHighlightsInCustomWindow(windowRegionX, windowRegionZ, windowRegionSize, dimension);
    }

    private ListenableFuture<?> initializeTask = Futures.immediateVoidFuture();

    @Override
    public void handleWorldChange(final XaeroWorldChangeEvent event) {
        if (XaeroWorldMapCore.currentSession == null) return;
        parentExecutor.execute(() -> {
            switch (event.worldChangeType()) {
                case ENTER_WORLD -> {
                    if (!cacheReady.get() && initializeTask.isDone()) {
                        initializeTask = initializeWorld();
                        Futures.addCallback(initializeTask, new FutureCallback() {
                            @Override
                            public void onSuccess(@Nullable final Object result) {
                                cacheReady.compareAndSet(false, true);
                            }

                            @Override
                            public void onFailure(@NotNull final Throwable t) {
                                if (t instanceof CancellationException) {
                                    XaeroPlus.LOGGER.warn("{} disk cache initialization cancelled", name);
                                } else {
                                    XaeroPlus.LOGGER.error("Error initializing {} disk cache", name, t);
                                }
                                cacheReady.set(false);
                                reset();
                            }
                        }, parentExecutor);
                    } else {
                        XaeroPlus.LOGGER.warn("[{}] Entered world when cache was already initialized", name);
                    }
                }
                case EXIT_WORLD -> {
                    if (!initializeTask.isDone()) {
                        initializeTask.cancel(true);
                    }
                    // make sure we mark as unready to prevent further mutations
                    if (cacheReady.compareAndSet(true, false)) {
                        try {
                            var future = CompletableFuture.allOf(flushAllChunks().toArray(CompletableFuture[]::new));
                            Wait.waitUntil(() -> !mc.isRunning() || future.isDone(), 30);
                        } catch (final Exception e) {
                            XaeroPlus.LOGGER.error("Error saving all chunks before disabling", e);
                        }
                    } else {
                        XaeroPlus.LOGGER.warn("[{}] Exited world when cache was already uninitialized", name);
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
            try {
                this.dbExecutor.shutdown();
                if (!this.dbExecutor.awaitTermination(6L, TimeUnit.SECONDS)) {
                    throw new RuntimeException("Timed out awaiting shutdown termination");
                }
            } catch (final Throwable e) {
                XaeroPlus.LOGGER.error("Timed out waiting for {} executor to shutdown", name, e);
                try {
                    var droppedTasks = this.dbExecutor.shutdownNow();
                    if (!this.dbExecutor.awaitTermination(4L, TimeUnit.SECONDS)) {
                        throw new RuntimeException("Timed out awaiting force shutdown termination");
                    }
                    XaeroPlus.LOGGER.error("Forcibly shut down {} executor with {} tasks remaining",
                        name, droppedTasks.size());
                } catch (final Throwable e2) {
                    XaeroPlus.LOGGER.error("Error force shutting down {} executor", name, e2);
                }
            }
        }
        if (this.database != null) this.database.close();
        // dimension cache instances will be GC'd, no need to explicitly clear them
        this.dimensionCacheMap.clear();
        this.database = null;
        this.initOperationQueue.clear();
    }

    // note: writes occur on the worker thread
    private List<CompletableFuture<?>> flushAllChunks() {
        return getAllCaches().stream()
            .map(cache -> submitTickTask(() -> {
                cache.flushStaleToRemoveChunks();
                cache.writeStaleHighlightsToDatabase();
            }))
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
            XaeroPlus.LOGGER.error("[{}] Unable to initialize {} disk cache handler for: {}, database: {} or executor: {} is null", Thread.currentThread().getName(),
                name, dimension.location(), db, executor);
            return null;
        }
        var cacheHandler = new ChunkHighlightCacheDimensionHandler(name, dimension, db, executor);
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
            XaeroPlus.LOGGER.info("Initializing {} disk cache for dimension: {}", name, dimension.location());
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
    private synchronized ListenableFuture<?> initializeWorld() {
        try {
            var currentSession = XaeroWorldMapCore.currentSession;
            if (currentSession == null) return Futures.immediateFailedFuture(new IllegalStateException("WorldMapSession is null"));
            MapProcessor mapProcessor = currentSession.getMapProcessor();
            if (mapProcessor == null) return Futures.immediateFailedFuture(new IllegalStateException("MapProcessor is null"));
            final String worldId = mapProcessor.getCurrentWorldId();
            if (worldId == null) return Futures.immediateFailedFuture(new IllegalStateException("WorldId is null"));
            this.currentWorldId = worldId;
            this.dbExecutor = MoreExecutors.listeningDecorator(
                Executors.newSingleThreadExecutor(
                    new ThreadFactoryBuilder()
                        .setNameFormat(name + "-Worker")
                        .setUncaughtExceptionHandler((t, e) -> {
                            XaeroPlus.LOGGER.error("Uncaught exception handler in {}", t.getName(), e);
                        })
                        .build()));
            return this.dbExecutor.submit(() -> {
                this.database = new ChunkHighlightDatabase(worldId, name);
                this.database.initializeDb();
                initializeDimensionCacheHandler(OVERWORLD);
                initializeDimensionCacheHandler(NETHER);
                initializeDimensionCacheHandler(END);
                loadChunksInViewedDimension();
                if (!initOperationQueue.isEmpty()) XaeroPlus.LOGGER.info("[{}] Running {} queued tasks",
                    name, initOperationQueue.size());
                while (!this.initOperationQueue.isEmpty()) {
                    var op = this.initOperationQueue.poll();
                    if (op == null || op.task() == null) continue;
                    submitTickTask(op.task());
                }
            });
        } catch (final Exception e) {
            reset(); // ensure we don't leave ourselves in a half init state somehow
            return Futures.immediateFailedFuture(e);
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
                var future = CompletableFuture.allOf(flushAllChunks().toArray(CompletableFuture[]::new));
                Wait.waitUntil(() -> !mc.isRunning() || future.isDone(), 30);
            } catch (final Exception e) {
                XaeroPlus.LOGGER.error("Error saving all chunks before disabling", e);
            }
            reset();
        });
    }

    public CompletableFuture<Void> onShutdown() {
        if (!mc.isSameThread()) {
            onDisable();
            return CompletableFuture.completedFuture(null);
        }
        cacheReady.set(false);
        for (var cache : getAllCaches()) {
            cache.flushStaleToRemoveChunks();
            cache.writeStaleHighlightsToDatabase();
        }
        return CompletableFuture.runAsync(this::closeAndAwaitTermination);
    }

    public int getMinimapRegionWindowSize() {
        return Math.max(3, Globals.minimapScaleMultiplier);
    }

    final Timer tickTimer = Timers.tickTimer();
    final Timer flushTimer = Timers.tickTimer();

    @Override
    public void handleTick() {
        if (!cacheReady.get()) return;
        if (XaeroWorldMapCore.currentSession == null) return;
        // reduce likelihood of all caches updating at the same time
        // changing the window involves iterating through every chunk in the cache to find which are now outside the window
        // which can be expensive if there are thousands of cache entries
        // this does make the update interval setting kind of a lie, but its for the best
        int jitter = ThreadLocalRandom.current().nextInt(0, 10);

        if (flushTimer.tick(600 + jitter)) {
            // periodically flush stale chunks to db
            // in case player is not moving, so window is not changing
            flushAllChunks();
        }
        // only update window on an interval
        if (!tickTimer.tick(10 + jitter)) {
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
        parentExecutor.execute(() -> {
            var dbExec = dbExecutor;
            if (dbExec != null && !dbExec.isShutdown()) {
                dbExec.execute(() -> {
                    if (database != null) database.close();
                });
                dbExec.shutdown();
            }
        });
        parentExecutor.shutdown();
    }

    public void closeAndAwaitTermination() {
        parentExecutor.execute(() -> {
            var dbExec = dbExecutor;
            if (dbExec != null && !dbExec.isShutdown()) {
                dbExec.execute(() -> {
                    if (database != null) database.close();
                });
                dbExec.shutdown();
                try {
                    if (!dbExec.awaitTermination(5L, TimeUnit.SECONDS)) {
                        throw new RuntimeException("Timed out awaiting shutdown termination");
                    }
                } catch (Exception e) {
                    XaeroPlus.LOGGER.error("Error waiting for {} db executor to shutdown", name, e);
                }
            }
        });
        parentExecutor.shutdown();
        try {
            if (!parentExecutor.awaitTermination(5L, TimeUnit.SECONDS)) {
                throw new RuntimeException("Timed out awaiting shutdown termination");
            }
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error waiting for {} executor to shutdown", name, e);
        }
    }

    private synchronized void addInitOperation(final Runnable task) {
        if (!initOperationQueue.isEmpty()) {
            var cutoff = Instant.now().minusSeconds(10);
            while (true) {
                var next = initOperationQueue.peek();
                if (next == null) break;
                if (next.time().isBefore(cutoff)) {
                    initOperationQueue.poll();
                } else {
                    break;
                }
            }
        }
        if (initOperationQueue.size() > 5000) {
            return;
        }
        initOperationQueue.add(new QueuedInitOperation(task));
    }

    record QueuedInitOperation(Instant time, Runnable task) {
        public QueuedInitOperation(Runnable task) {
            this(Instant.now(), task);
        }
    }
}
