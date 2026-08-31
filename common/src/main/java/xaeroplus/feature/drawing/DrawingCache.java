package xaeroplus.feature.drawing;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import it.unimi.dsi.fastutil.longs.*;
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
import xaeroplus.feature.drawing.db.DrawingDatabase;
import xaeroplus.feature.render.ellipse.Ellipse;
import xaeroplus.feature.render.line.Line;
import xaeroplus.feature.render.text.Text;
import xaeroplus.module.impl.TickTaskExecutor;
import xaeroplus.util.ChunkUtils;
import xaeroplus.util.Wait;
import xaeroplus.util.timer.Timer;
import xaeroplus.util.timer.Timers;

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
    private final Map<ResourceKey<Level>, DrawingHighlightCacheDimensionHandler> highlightsCacheMap = new ConcurrentHashMap<>(3);
    private final Map<ResourceKey<Level>, DrawingLinesCacheDimensionHandler> linesCacheMap = new ConcurrentHashMap<>(3);
    private final Map<ResourceKey<Level>, DrawingEllipseCacheDimensionHandler> ellipsesCacheMap = new ConcurrentHashMap<>(3);
    private final Map<ResourceKey<Level>, DrawingTextCacheDimensionHandler> textsCacheMap = new ConcurrentHashMap<>(3);
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
            DrawingHighlightCacheDimensionHandler cacheForActualDimension = getHighlightCacheForDimension(dimension, true);
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

    public void addEllipse(final Ellipse ellipse, final int color, final ResourceKey<Level> dimension) {
        try {
            var cache = getEllipseCacheForDimension(dimension, true);
            if (cache == null) {
                initializeTaskQueue.add(() -> addEllipse(ellipse, color, dimension));
                return;
            }
            cache.addEllipse(ellipse, color);
        } catch (final Exception e) {
            XaeroPlus.LOGGER.warn("Error adding ellipse to {} disk cache: {}, {}", databaseName, ellipse, e);
        }
    }

    public void addText(Text text, ResourceKey<Level> dimension) {
        if (text.value().isBlank()) return;
        try {
            DrawingTextCacheDimensionHandler cacheForActualDimension = getTextCacheForDimension(dimension, true);
            if (cacheForActualDimension == null) {
                // if the cache is not ready yet, queue the line to be added
                initializeTaskQueue.add(() -> addText(text, dimension));
                return;
            }
            cacheForActualDimension.addText(text);
        } catch (final Exception e) {
            XaeroPlus.LOGGER.warn("Error adding text to {} disk cache: {}, {}", databaseName, text, e);
        }
    }

    public void removeHighlight(final int x, final int z, final ResourceKey<Level> dimension) {
        try {
            DrawingHighlightCacheDimensionHandler cacheForActualDimension = getHighlightCacheForDimension(dimension, true);
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

    public void removeHighlights(final LongCollection toRemove, final ResourceKey<Level> dimension) {
        try {
            var cacheForActualDimension = getHighlightCacheForDimension(dimension, true);
            if (cacheForActualDimension == null) {
                initializeTaskQueue.add(() -> removeHighlights(toRemove, dimension));
                return;
            }
            cacheForActualDimension.removeHighlights(toRemove);
        } catch (final Exception e) {
            XaeroPlus.LOGGER.warn("Error removing {} highlights from {} disk cache: {}", toRemove.size(), databaseName, toRemove, e);
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

    public void removeEllipse(final Ellipse ellipse, final ResourceKey<Level> dimension) {
        try {
            var cache = getEllipseCacheForDimension(dimension, true);
            if (cache == null) {
                initializeTaskQueue.add(() -> removeEllipse(ellipse, dimension));
                return;
            }
            cache.removeEllipse(ellipse);
        } catch (final Exception e) {
            XaeroPlus.LOGGER.warn("Error removing ellipse from {} disk cache: {}, {}", databaseName, ellipse, e);
        }
    }

    public void removeText(final int x, final int z, ResourceKey<Level> dimension) {
        try {
            DrawingTextCacheDimensionHandler cacheForActualDimension = getTextCacheForDimension(dimension, true);
            if (cacheForActualDimension == null) {
                // if the cache is not ready yet, queue the line to be removed
                initializeTaskQueue.add(() -> removeText(x, z, dimension));
                return;
            }
            cacheForActualDimension.removeText(x, z);
        } catch (final Exception e) {
            XaeroPlus.LOGGER.warn("Error removing text from {} disk cache: {}, {}", databaseName, x, z, e);
        }
    }

    public Long2LongMap getHighlights(final ResourceKey<Level> dimensionId) {
        if (dimensionId == null) return Long2LongMaps.EMPTY_MAP;
        DrawingHighlightCacheDimensionHandler cacheForDimension = getHighlightCacheForDimension(dimensionId, false);
        if (cacheForDimension == null) return Long2LongMaps.EMPTY_MAP;
        return cacheForDimension.getCacheMap(dimensionId);
    }

    public Object2IntMap<Line> getLines(final ResourceKey<Level> dimension) {
        if (dimension == null) return Object2IntMaps.emptyMap();
        var cacheForDimension = getLinesCacheForDimension(dimension, false);
        if (cacheForDimension == null) return Object2IntMaps.emptyMap();
        return cacheForDimension.getLines();
    }

    public Object2IntMap<Ellipse> getEllipses(final ResourceKey<Level> dimension) {
        if (dimension == null) return Object2IntMaps.emptyMap();
        var cacheForDimension = getEllipseCacheForDimension(dimension, false);
        if (cacheForDimension == null) return Object2IntMaps.emptyMap();
        return cacheForDimension.getEllipses();
    }

    public Long2ObjectMap<Text> getTexts(final ResourceKey<Level> dimensionId) {
        if (dimensionId == null) return Long2ObjectMaps.emptyMap();
        var cacheForDimension = getTextCacheForDimension(dimensionId, false);
        if (cacheForDimension == null) return Long2ObjectMaps.emptyMap();
        return cacheForDimension.getTexts();
    }

    public void handleWorldChange(final XaeroWorldChangeEvent event) {
        parentExecutor.execute(() -> {
            switch (event.worldChangeType()) {
                case ENTER_WORLD -> {
                    if (!cacheReady.get()) {
                        if (initializeWorld()) {
                            cacheReady.set(true);
                            submitTickTask(() -> {
                                loadHighlightsInViewedDimension();
                                loadLinesInViewedDimension();
                                loadEllipsesInViewedDimension();
                                loadTextsInViewedDimension();
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
                            List<CompletableFuture<?>> tasks = new ArrayList<>();
                            tasks.addAll(flushAllChunks());
                            tasks.addAll(flushAllLines());
                            tasks.addAll(flushAllEllipses());
                            tasks.addAll(flushAllTexts());
                            var future = CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new));
                            Wait.waitUntil(() -> !mc.isRunning() || future.isDone(), 30);
                        } catch (final Exception e) {
                            XaeroPlus.LOGGER.error("Error saving all chunks before world change", e);
                        }
                    } else {
                        XaeroPlus.LOGGER.warn("[{}] Exited world when cache was already uninitialized", databaseName);
                    }
                    reset();
                }
                case VIEWED_DIMENSION_SWITCH -> {
                    submitTickTask(this::loadHighlightsInViewedDimension);
                    submitTickTask(this::loadLinesInViewedDimension);
                    submitTickTask(this::loadEllipsesInViewedDimension);
                    submitTickTask(this::loadTextsInViewedDimension);
                }
                case ACTUAL_DIMENSION_SWITCH -> {
                    submitTickTask(this::loadChunksOnActualDimensionSwitch);
                    submitTickTask(this::loadLinesOnActualDimensionSwitch);
                    submitTickTask(this::loadEllipsesOnActualDimensionSwitch);
                    submitTickTask(this::loadTextsOnActualDimensionSwitch);
                }
            }
        });
    }

    private CompletableFuture<?> submitTickTask(final Runnable runnable) {
        return TickTaskExecutor.INSTANCE.submit(runnable);
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
                XaeroPlus.LOGGER.error("Timed out waiting for {} executor to shutdown", databaseName, e);
                try {
                    var droppedTasks = this.dbExecutor.shutdownNow();
                    if (!this.dbExecutor.awaitTermination(4L, TimeUnit.SECONDS)) {
                        throw new RuntimeException("Timed out awaiting force shutdown termination");
                    }
                    XaeroPlus.LOGGER.error("Forcibly shut down {} executor with {} tasks remaining", databaseName, droppedTasks.size());
                } catch (final Throwable e2) {
                    XaeroPlus.LOGGER.error("Error force shutting down {} executor", databaseName, e2);
                }
            }
        }
        if (this.database != null) this.database.close();
        // dimension cache instances will be GC'd, no need to explicitly clear them
        this.highlightsCacheMap.clear();
        this.linesCacheMap.clear();
        this.ellipsesCacheMap.clear();
        this.textsCacheMap.clear();
        this.database = null;
        this.initializeTaskQueue.clear();
    }

    // note: writes occur on the worker thread
    private List<CompletableFuture<?>> flushAllChunks() {
        return getAllHighlightCaches().stream()
            .map(cache -> submitTickTask(cache::writeStaleHighlightsToDatabase))
            .collect(Collectors.toList());
    }

    private List<CompletableFuture<?>> flushAllLines() {
        return getAllLinesCaches().stream()
            .map(cache -> submitTickTask(cache::writeStaleLinesToDatabase))
            .collect(Collectors.toList());
    }

    private List<CompletableFuture<?>> flushAllEllipses() {
        return getAllEllipseCaches().stream()
            .map(cache -> submitTickTask(cache::writeStaleEllipsesToDatabase))
            .collect(Collectors.toList());
    }

    private List<CompletableFuture<?>> flushAllTexts() {
        return getAllTextsCaches().stream()
            .map(cache -> submitTickTask(cache::writeStaleTextsToDatabase))
            .collect(Collectors.toList());
    }

    public DrawingHighlightCacheDimensionHandler getHighlightCacheForActualDimension() {
        if (!cacheReady.get()) return null;
        return getHighlightCacheForDimension(ChunkUtils.getActualDimension(), true);
    }

    public DrawingTextCacheDimensionHandler getTextCacheForActualDimension() {
        if (!cacheReady.get()) return null;
        return getTextCacheForDimension(ChunkUtils.getActualDimension(), true);
    }

    private DrawingHighlightCacheDimensionHandler initializeHighlightDimensionCacheHandler(final ResourceKey<Level> dimension) {
        if (dimension == null) return null;
        var db = this.database;
        var executor = this.dbExecutor;
        if (db == null || executor == null) {
            XaeroPlus.LOGGER.error("[{}] Unable to initialize {} disk cache handler for: {}, database: {} or executor: {} is null", Thread.currentThread().getName(), databaseName, dimension.location(), db, executor);
            return null;
        }
        var cacheHandler = new DrawingHighlightCacheDimensionHandler(databaseName, dimension, db, executor);
        db.initializeDimension(dimension);
        this.highlightsCacheMap.put(dimension, cacheHandler);
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

    private DrawingEllipseCacheDimensionHandler initializeEllipseCacheHandler(final ResourceKey<Level> dimension) {
        if (dimension == null) return null;
        var db = database;
        var executor = dbExecutor;
        if (db == null || executor == null) {
            XaeroPlus.LOGGER.error("[{}] Unable to initialize {} ellipse disk cache handler for: {}", Thread.currentThread().getName(), databaseName, dimension.location());
            return null;
        }
        var ellipsesCacheHandler = new DrawingEllipseCacheDimensionHandler(dimension, db, executor);
        db.initializeDimension(dimension);
        ellipsesCacheMap.put(dimension, ellipsesCacheHandler);
        return ellipsesCacheHandler;
    }

    private DrawingTextCacheDimensionHandler initializeTextDimensionCacheHandler(final ResourceKey<Level> dimension) {
        if (dimension == null) return null;
        var db = this.database;
        var executor = this.dbExecutor;
        if (db == null || executor == null) {
            XaeroPlus.LOGGER.error("[{}] Unable to initialize {} disk cache handler for: {}, database: {} or executor: {} is null", Thread.currentThread().getName(), databaseName, dimension.location(), db, executor);
            return null;
        }
        var cacheHandler = new DrawingTextCacheDimensionHandler(dimension, db, executor);
        db.initializeDimension(dimension);
        this.textsCacheMap.put(dimension, cacheHandler);
        return cacheHandler;
    }

    public DrawingHighlightCacheDimensionHandler getHighlightCacheForDimension(final ResourceKey<Level> dimension, boolean create) {
        if (!cacheReady.get()) return null;
        if (dimension == null) return null;
        var dimensionCache = highlightsCacheMap.get(dimension);
        if (dimensionCache == null) {
            if (!create) return null;
            XaeroPlus.LOGGER.info("Initializing {} disk cache for dimension: {}", databaseName, dimension.location());
            dimensionCache = initializeHighlightDimensionCacheHandler(dimension);
        }
        return dimensionCache;
    }

    public DrawingTextCacheDimensionHandler getTextCacheForDimension(final ResourceKey<Level> dimension, boolean create) {
        if (!cacheReady.get()) return null;
        if (dimension == null) return null;
        var dimensionCache = textsCacheMap.get(dimension);
        if (dimensionCache == null) {
            if (!create) return null;
            XaeroPlus.LOGGER.info("Initializing {} disk cache for dimension: {}", databaseName, dimension.location());
            dimensionCache = initializeTextDimensionCacheHandler(dimension);
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

    public DrawingEllipseCacheDimensionHandler getEllipseCacheForDimension(final ResourceKey<Level> dimension, final boolean create) {
        if (!cacheReady.get() || dimension == null) return null;
        var cache = ellipsesCacheMap.get(dimension);
        if (cache == null && create) {
            XaeroPlus.LOGGER.info("Initializing {} disk ellipse cache for dimension: {}", databaseName, dimension.location());
            cache = initializeEllipseCacheHandler(dimension);
        }
        return cache;
    }

    public List<DrawingHighlightCacheDimensionHandler> getAllHighlightCaches() {
        return List.copyOf(highlightsCacheMap.values());
    }

    public List<DrawingLinesCacheDimensionHandler> getAllLinesCaches() {
        return List.copyOf(linesCacheMap.values());
    }

    public List<DrawingEllipseCacheDimensionHandler> getAllEllipseCaches() {
        return List.copyOf(ellipsesCacheMap.values());
    }

    public List<DrawingTextCacheDimensionHandler> getAllTextsCaches() {
        return List.copyOf(textsCacheMap.values());
    }

    public List<DrawingHighlightCacheDimensionHandler> getHighlightCachesExceptDimension(final ResourceKey<Level> dimension) {
        var caches = new ArrayList<DrawingHighlightCacheDimensionHandler>(highlightsCacheMap.size());
        for (var entry : highlightsCacheMap.entrySet()) {
            if (!entry.getKey().equals(dimension)) {
                caches.add(entry.getValue());
            }
        }
        return caches;
    }

    public List<DrawingHighlightCacheDimensionHandler> getHighlightCachesExceptDimensions(final List<ResourceKey<Level>> dimensions) {
        var caches = new ArrayList<DrawingHighlightCacheDimensionHandler>(highlightsCacheMap.size());
        for (var entry : highlightsCacheMap.entrySet()) {
            if (!dimensions.contains(entry.getKey())) {
                caches.add(entry.getValue());
            }
        }
        return caches;
    }

    public List<DrawingTextCacheDimensionHandler> getTextCachesExceptDimension(final ResourceKey<Level> dimension) {
        var caches = new ArrayList<DrawingTextCacheDimensionHandler>(textsCacheMap.size());
        for (var entry : textsCacheMap.entrySet()) {
            if (!entry.getKey().equals(dimension)) {
                caches.add(entry.getValue());
            }
        }
        return caches;
    }

    public List<DrawingTextCacheDimensionHandler> getTextCachesExceptDimensions(final List<ResourceKey<Level>> dimensions) {
        var caches = new ArrayList<DrawingTextCacheDimensionHandler>(textsCacheMap.size());
        for (var entry : textsCacheMap.entrySet()) {
            if (!dimensions.contains(entry.getKey())) {
                caches.add(entry.getValue());
            }
        }
        return caches;
    }

    public List<DrawingLinesCacheDimensionHandler> getLineCachesExceptDimension(final ResourceKey<Level> dimension) {
        var caches = new ArrayList<DrawingLinesCacheDimensionHandler>(linesCacheMap.size());
        for (var entry : linesCacheMap.entrySet()) {
            if (!entry.getKey().equals(dimension)) {
                caches.add(entry.getValue());
            }
        }
        return caches;
    }

    public List<DrawingLinesCacheDimensionHandler> getLineCachesExceptDimensions(final List<ResourceKey<Level>> dimensions) {
        var caches = new ArrayList<DrawingLinesCacheDimensionHandler>(linesCacheMap.size());
        for (var entry : linesCacheMap.entrySet()) {
            if (!dimensions.contains(entry.getKey())) {
                caches.add(entry.getValue());
            }
        }
        return caches;
    }

    public List<DrawingEllipseCacheDimensionHandler> getEllipseCachesExceptDimension(final ResourceKey<Level> dimension) {
        var caches = new ArrayList<DrawingEllipseCacheDimensionHandler>(ellipsesCacheMap.size());
        for (var entry : ellipsesCacheMap.entrySet()) {
            if (!entry.getKey().equals(dimension)) caches.add(entry.getValue());
        }
        return caches;
    }

    public List<DrawingEllipseCacheDimensionHandler> getEllipseCachesExceptDimensions(final List<ResourceKey<Level>> dimensions) {
        var caches = new ArrayList<DrawingEllipseCacheDimensionHandler>(ellipsesCacheMap.size());
        for (var entry : ellipsesCacheMap.entrySet()) {
            if (!dimensions.contains(entry.getKey())) caches.add(entry.getValue());
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
            initializeHighlightDimensionCacheHandler(OVERWORLD);
            initializeHighlightDimensionCacheHandler(NETHER);
            initializeHighlightDimensionCacheHandler(END);
            initializeLinesCacheHandler(OVERWORLD);
            initializeLinesCacheHandler(NETHER);
            initializeLinesCacheHandler(END);
            initializeEllipseCacheHandler(OVERWORLD);
            initializeEllipseCacheHandler(NETHER);
            initializeEllipseCacheHandler(END);
            initializeTextDimensionCacheHandler(OVERWORLD);
            initializeTextDimensionCacheHandler(NETHER);
            initializeTextDimensionCacheHandler(END);
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
        var cacheForActualDimension = getHighlightCacheForActualDimension();
        if (cacheForActualDimension == null) return;
        cacheForActualDimension
            .setWindow(ChunkUtils.actualPlayerRegionX(), ChunkUtils.actualPlayerRegionZ(), getMinimapRegionWindowSize());
    }

    private void loadLinesOnActualDimensionSwitch() {
        var linesCacheForActualDimension = getLinesCacheForDimension(ChunkUtils.getActualDimension(), true);
        if (linesCacheForActualDimension == null) return;
        linesCacheForActualDimension
            .setWindow(ChunkUtils.actualPlayerRegionX(), ChunkUtils.actualPlayerRegionZ(), getMinimapRegionWindowSize());
    }

    private void loadEllipsesOnActualDimensionSwitch() {
        var cache = getEllipseCacheForDimension(ChunkUtils.getActualDimension(), true);
        if (cache != null) {
            cache.setWindow(ChunkUtils.actualPlayerRegionX(), ChunkUtils.actualPlayerRegionZ(), getMinimapRegionWindowSize());
        }
    }

    private void loadTextsOnActualDimensionSwitch() {
        var cacheForActualDimension = getTextCacheForActualDimension();
        if (cacheForActualDimension == null) return;
        cacheForActualDimension
            .setWindow(ChunkUtils.actualPlayerRegionX(), ChunkUtils.actualPlayerRegionZ(), getMinimapRegionWindowSize());
    }

    private void loadHighlightsInViewedDimension() {
        var viewedDim = Globals.getCurrentDimensionId();
        var cacheForCurrentDimension = getHighlightCacheForDimension(viewedDim, true);
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
        linesCacheForCurrentDimension.setWindow(windowCenterX, windowCenterZ, windowSize);
    }

    private void loadEllipsesInViewedDimension() {
        var viewedDimension = Globals.getCurrentDimensionId();
        var cache = getEllipseCacheForDimension(viewedDimension, true);
        if (cache == null) return;
        final int windowSize;
        final int windowCenterX;
        final int windowCenterZ;
        var guiMap = getGuiMap();
        if (guiMap.isPresent()) {
            windowSize = getGuiMapRegionSize(guiMap.get());
            windowCenterX = getGuiMapCenterRegionX(guiMap.get());
            windowCenterZ = getGuiMapCenterRegionZ(guiMap.get());
        } else {
            windowSize = getMinimapRegionWindowSize();
            windowCenterX = ChunkUtils.getPlayerRegionX();
            windowCenterZ = ChunkUtils.getPlayerRegionZ();
        }
        cache.setWindow(windowCenterX, windowCenterZ, windowSize);
    }

    private void loadTextsInViewedDimension() {
        var viewedDim = Globals.getCurrentDimensionId();
        var cacheForCurrentDimension = getTextCacheForDimension(viewedDim, true);
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

    public void onEnable() {
        handleWorldChange(new XaeroWorldChangeEvent(ENTER_WORLD, null, ChunkUtils.getActualDimension()));
    }

    public void onDisable() {
        parentExecutor.execute(() -> {
            cacheReady.set(false);
            try {
                List<CompletableFuture<?>> tasks = new ArrayList<>();
                tasks.addAll(flushAllChunks());
                tasks.addAll(flushAllLines());
                tasks.addAll(flushAllEllipses());
                tasks.addAll(flushAllTexts());
                var future = CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new));
                Wait.waitUntil(() -> !mc.isRunning() || future.isDone(), 30);
            } catch (final Exception e) {
                XaeroPlus.LOGGER.error("Error saving all drawing data before disabling", e);
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
        for (var cache : getAllHighlightCaches()) {
            cache.writeStaleHighlightsToDatabase();
        }
        for (var cache : getAllLinesCaches()) {
            cache.writeStaleLinesToDatabase();
        }
        for (var cache : getAllEllipseCaches()) {
            cache.writeStaleEllipsesToDatabase();
        }
        for (var cache : getAllTextsCaches()) {
            cache.writeStaleTextsToDatabase();
        }
        return CompletableFuture.runAsync(this::closeAndAwaitTermination);
    }

    public int getMinimapRegionWindowSize() {
        return Math.max(3, Globals.minimapScaleMultiplier);
    }

    final Timer tickTimer = Timers.tickTimer();
    final Timer flushTimer = Timers.tickTimer();

    public void handleTick() {
        if (!cacheReady.get()) return;
        if (XaeroWorldMapCore.currentSession == null) return;
        // reduce likelihood of all caches updating at the same time
        // changing the window involves iterating through every chunk in the cache to find which are now outside the window
        // which can be expensive if there are thousands of cache entries
        // this does make the update interval setting kind of a lie, but its for the best
        int jitter = ThreadLocalRandom.current().nextInt(0, 10);
        if (flushTimer.tick(600 + jitter)) {
            // periodically flush stale drawings to db
            // in case player is not moving, so window is not changing
            flushAllChunks();
            flushAllLines();
            flushAllEllipses();
            flushAllTexts();
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
        var highlightCacheForDimension = getHighlightCacheForDimension(mapDimension, true);
        if (highlightCacheForDimension != null) highlightCacheForDimension.setWindow(windowCenterX, windowCenterZ, windowSize);
        var textCacheForDimension = getTextCacheForDimension(mapDimension, true);
        if (textCacheForDimension != null) textCacheForDimension.setWindow(windowCenterX, windowCenterZ, windowSize);
        var lineCacheForDimension = getLinesCacheForDimension(mapDimension, true);
        if (lineCacheForDimension != null) lineCacheForDimension.setWindow(windowCenterX, windowCenterZ, windowSize);
        var ellipseCacheForDimension = getEllipseCacheForDimension(mapDimension, true);
        if (ellipseCacheForDimension != null) ellipseCacheForDimension.setWindow(windowCenterX, windowCenterZ, windowSize);
        if (mapDimension == actualDimension) {
            getHighlightCachesExceptDimension(mapDimension)
                .forEach(cache -> cache.setWindow(0, 0, 0));
            getTextCachesExceptDimension(mapDimension)
                .forEach(cache -> cache.setWindow(0, 0, 0));
            getLineCachesExceptDimension(mapDimension)
                .forEach(cache -> cache.setWindow(0, 0, 0));
            getEllipseCachesExceptDimension(mapDimension)
                .forEach(cache -> cache.setWindow(0, 0, 0));
        } else {
            var actualDimHighlightCache = getHighlightCacheForDimension(actualDimension, true);
            if (actualDimHighlightCache != null) {
                actualDimHighlightCache.setWindow(actualPlayerRegionX, actualPlayerRegionZ, windowSize);
            }
            var actualDimTextCache = getTextCacheForDimension(actualDimension, true);
            if (actualDimTextCache != null) {
                actualDimTextCache.setWindow(actualPlayerRegionX, actualPlayerRegionZ, windowSize);
            }
            var actualDimLinesCache = getLinesCacheForDimension(actualDimension, true);
            if (actualDimLinesCache != null) {
                actualDimLinesCache.setWindow(actualPlayerRegionX, actualPlayerRegionZ, windowSize);
            }
            var actualDimEllipseCache = getEllipseCacheForDimension(actualDimension, true);
            if (actualDimEllipseCache != null) {
                actualDimEllipseCache.setWindow(actualPlayerRegionX, actualPlayerRegionZ, windowSize);
            }
            getHighlightCachesExceptDimensions(List.of(mapDimension, actualDimension))
                .forEach(cache -> cache.setWindow(0, 0, 0));
            getTextCachesExceptDimensions(List.of(mapDimension, actualDimension))
                .forEach(cache -> cache.setWindow(0, 0, 0));
            getLineCachesExceptDimensions(List.of(mapDimension, actualDimension))
                .forEach((cache -> cache.setWindow(0, 0, 0)));
            getEllipseCachesExceptDimensions(List.of(mapDimension, actualDimension))
                .forEach(cache -> cache.setWindow(0, 0, 0));
        }
    }

    @Override
    public void close() throws IOException {
        // does not await the shutdown
        // this saving cache instance should never be reused after this is called
        parentExecutor.execute(() -> {
            var dbExec = this.dbExecutor;
            if (dbExec != null && !dbExec.isShutdown()) {
                dbExec.execute(() -> {
                    if (database != null) {
                        database.close();
                    }
                });
                dbExec.shutdown();
            }
        });
        parentExecutor.shutdown();
    }

    public void closeAndAwaitTermination() {
        parentExecutor.execute(() -> {
            var dbExec = this.dbExecutor;
            if (dbExec != null && !dbExec.isShutdown()) {
                dbExec.execute(() -> {
                    if (database != null) {
                        database.close();
                    }
                });
                dbExec.shutdown();
                try {
                    if (!dbExec.awaitTermination(5L, TimeUnit.SECONDS)) {
                        throw new RuntimeException("Failed to shutdown drawing executor");
                    }
                } catch (Exception e) {
                    XaeroPlus.LOGGER.error("Error waiting for drawing db executor to shutdown", e);
                }
            }
        });
        parentExecutor.shutdown();
        try {
            if (!parentExecutor.awaitTermination(5L, TimeUnit.SECONDS)) {
                throw new RuntimeException("Failed to shutdown drawing executor");
            }
        } catch (Exception e) {
            XaeroPlus.LOGGER.error("Error waiting for executor to shutdown", e);
        }
    }
}
