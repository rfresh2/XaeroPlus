package xaeroplus.feature.render.highlights;

import com.google.common.util.concurrent.*;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xaero.map.core.XaeroWorldMapCore;
import xaero.map.gui.GuiMap;
import xaeroplus.Globals;
import xaeroplus.XaeroPlus;
import xaeroplus.util.ChunkUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static net.minecraft.world.level.Level.*;
import static xaeroplus.util.ChunkUtils.getActualDimension;
import static xaeroplus.util.GuiMapHelper.*;

public class ChunkHighlightSavingCache implements ChunkHighlightCache {
    // these are initialized lazily
    @Nullable private ChunkHighlightDatabase database = null;
    private static final int defaultRegionWindowSize = 2; // when we are only viewing the minimap
    @Nullable private String currentWorldId;
    private boolean worldCacheInitialized = false;
    @Nullable private final String databaseName;
    @Nullable private ListeningExecutorService executorService;
    private final Map<ResourceKey<Level>, ChunkHighlightCacheDimensionHandler> dimensionCacheMap = new ConcurrentHashMap<>(3);

    public ChunkHighlightSavingCache(final @NotNull String databaseName) {
        this.databaseName = databaseName;
    }

    public boolean addHighlight(final int x, final int z) {
        try {
            ChunkHighlightCacheDimensionHandler cacheForCurrentDimension = getCacheForCurrentDimension();
            if (cacheForCurrentDimension == null) throw new RuntimeException("Didn't find cache for current dimension");
            cacheForCurrentDimension.addHighlight(x, z);
            return true;
        } catch (final Exception e) {
            XaeroPlus.LOGGER.debug("Error adding highlight to saving cache: {}", databaseName, e);
            return false;
        }
    }

    public void addHighlight(final int x, final int z, final long foundTime, final ResourceKey<Level> dimension) {
        if (dimension == null) return;
        ChunkHighlightCacheDimensionHandler cacheForDimension = getCacheForDimension(dimension, true);
        if (cacheForDimension == null) return;
        cacheForDimension.addHighlight(x, z, foundTime);
    }

    public boolean removeHighlight(final int x, final int z) {
        try {
            ChunkHighlightCacheDimensionHandler cacheForCurrentDimension = getCacheForCurrentDimension();
            if (cacheForCurrentDimension == null) throw new RuntimeException("Didn't find cache for current dimension");
            cacheForCurrentDimension.removeHighlight(x, z);
            return true;
        } catch (final Exception e) {
            XaeroPlus.LOGGER.debug("Error removing highlight from saving cache: {}, {}", x, z, e);
            return false;
        }
    }

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

    public void handleWorldChange() {
        Futures.whenAllComplete(saveAllChunks())
                .call(() -> {
                    reset();
                    initializeWorld();
                    loadChunksInActualDimension();
                    return null;
                }, Globals.cacheRefreshExecutorService.get());
    }

    public void reset() {
        this.worldCacheInitialized = false;
        this.currentWorldId = null;
        if (this.executorService != null) this.executorService.shutdown();
        if (this.database != null) this.database.close();
        this.dimensionCacheMap.clear();
        this.database = null;
    }

    private List<ListenableFuture<?>> saveAllChunks() {
        if (!worldCacheInitialized) return Collections.emptyList();
        return getAllCaches().stream()
                .map(ChunkHighlightCacheDimensionHandler::writeAllHighlightsToDatabase)
                .collect(Collectors.toList());
    }

    public ChunkHighlightCacheDimensionHandler getCacheForCurrentDimension() {
        if (!worldCacheInitialized) return null;
        return getCacheForDimension(Globals.getCurrentDimensionId(), true);
    }

    private ChunkHighlightCacheDimensionHandler initializeDimensionCacheHandler(final ResourceKey<Level> dimension) {
        if (dimension == null) return null;
        var db = this.database;
        var executor = this.executorService;
        if (db == null || executor == null) {
            XaeroPlus.LOGGER.error("Unable to initialize dimension cache handler for: {}, database or executor is null", dimension.location());
            return null;
        }
        var cacheHandler = new ChunkHighlightCacheDimensionHandler(dimension, db, executor);
        db.initializeDimension(dimension);
        this.dimensionCacheMap.put(dimension, cacheHandler);
        return cacheHandler;
    }

    public ChunkHighlightCacheDimensionHandler getCacheForDimension(final ResourceKey<Level> dimension, boolean create) {
        if (!worldCacheInitialized) return null;
        if (dimension == null) return null;
        var dimensionCache = dimensionCacheMap.get(dimension);
        if (dimensionCache == null) {
            if (!create) return null;
            XaeroPlus.LOGGER.error("Initializing cache for dimension: {}", dimension.location());
            dimensionCache = initializeDimensionCacheHandler(dimension);
        }
        return dimensionCache;
    }

    private List<ChunkHighlightCacheDimensionHandler> getAllCaches() {
        return new ArrayList<>(dimensionCacheMap.values());
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

    private void initializeWorld() {
        try {
            final String worldId = XaeroWorldMapCore.currentSession.getMapProcessor().getCurrentWorldId();
            if (worldId == null) return;
            this.executorService = MoreExecutors.listeningDecorator(
                Executors.newSingleThreadExecutor(
                    new ThreadFactoryBuilder()
                        .setNameFormat("XaeroPlus-ChunkHighlightCacheHandler-" + currentWorldId)
                        .build()));
            this.currentWorldId = worldId;
            this.database = new ChunkHighlightDatabase(worldId, databaseName);
            initializeDimensionCacheHandler(OVERWORLD);
            initializeDimensionCacheHandler(NETHER);
            initializeDimensionCacheHandler(END);
            this.worldCacheInitialized = true;
            loadChunksInActualDimension();
        } catch (final Exception e) {
            // expected on game launch
        }
    }

    private void loadChunksInActualDimension() {
        ChunkHighlightCacheDimensionHandler cacheForCurrentDimension = getCacheForCurrentDimension();
        if (cacheForCurrentDimension == null) return;
        cacheForCurrentDimension
            .setWindow(ChunkUtils.actualPlayerRegionX(), ChunkUtils.actualPlayerRegionZ(), defaultRegionWindowSize);
    }

    public void onEnable() {
        if (!worldCacheInitialized) {
            initializeWorld();
        }
    }

    public void onDisable() {
        Futures.whenAllComplete(saveAllChunks()).call(() -> {
            reset();
            return null;
        }, Globals.cacheRefreshExecutorService.get());
    }

    @Override
    public Long2LongMap getHighlightsState() {
        return null;
    }

    @Override
    public void loadPreviousState(final Long2LongMap state) {

    }

    int tickCounter = 0;
    public void handleTick() {
        if (!worldCacheInitialized) return;
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

        Optional<GuiMap> guiMapOptional = getGuiMap();
        if (guiMapOptional.isPresent()) {
            final GuiMap guiMap = guiMapOptional.get();
            final ResourceKey<Level> mapDimension = Globals.getCurrentDimensionId();
            final int mapCenterX = getGuiMapCenterRegionX(guiMap);
            final int mapCenterZ = getGuiMapCenterRegionZ(guiMap);
            final int mapSize = getGuiMapRegionSize(guiMap);
            final ChunkHighlightCacheDimensionHandler cacheForDimension = getCacheForDimension(mapDimension, true);
            if (cacheForDimension != null) cacheForDimension.setWindow(mapCenterX, mapCenterZ, mapSize);
            getCachesExceptDimension(mapDimension)
                .forEach(cache -> cache.setWindow(0, 0, 0));
        } else {
            final ChunkHighlightCacheDimensionHandler cacheForDimension = getCacheForDimension(Globals.getCurrentDimensionId(), true);
            if (cacheForDimension != null) cacheForDimension.setWindow(ChunkUtils.getPlayerRegionX(), ChunkUtils.getPlayerRegionZ(), defaultRegionWindowSize);
            getCachesExceptDimension(Globals.getCurrentDimensionId())
                .forEach(cache -> cache.setWindow(0, 0, 0));
        }
    }
}
