package xaeroplus.util.highlights;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import xaero.map.core.XaeroWorldMapCore;
import xaero.map.gui.GuiMap;
import xaeroplus.XaeroPlus;
import xaeroplus.util.ChunkUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static xaeroplus.util.ChunkUtils.getActualDimension;
import static xaeroplus.util.GuiMapHelper.*;

public class ChunkHighlightSavingCache {
    // these are initialized async
    private ChunkHighlightCacheDimensionHandler netherCache = null;
    private ChunkHighlightCacheDimensionHandler overworldCache = null;
    private ChunkHighlightCacheDimensionHandler endCache = null;
    private ChunkHighlightDatabase database = null;
    private static final int defaultRegionWindowSize = 2; // when we are only viewing the minimap
    private String currentWorldId;
    private boolean worldCacheInitialized = false;
    private final String databaseName;

    public ChunkHighlightSavingCache(final String databaseName) {
        this.databaseName = databaseName;
    }

    public boolean addHighlight(final int x, final int z) {
        try {
            ChunkHighlightCacheDimensionHandler cacheForCurrentDimension = getCacheForCurrentDimension();
            if (cacheForCurrentDimension == null) throw new RuntimeException("Didn't find cache for current dimension");
            cacheForCurrentDimension.addHighlight(x, z);
            return true;
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error adding highlight to saving cache: {}", databaseName, e);
            return false;
        }
    }

    public void addHighlight(final int x, final int z, final long foundTime, final int dimension) {
        ChunkHighlightCacheDimensionHandler cacheForDimension = getCacheForDimension(dimension);
        if (cacheForDimension == null) throw new RuntimeException("Didn't find cache for dimension: " + dimension);
        cacheForDimension.addHighlight(x, z, foundTime);
    }

    public boolean isHighlighted(final int chunkPosX, final int chunkPosZ, final int dimensionId) {
        ChunkHighlightCacheDimensionHandler cacheForDimension = getCacheForDimension(dimensionId);
        if (cacheForDimension == null) return false;
        return cacheForDimension.isHighlighted(chunkPosX, chunkPosZ);
    }

    public boolean isHighlighted(final int chunkPosX, final int chunkPosZ) {
        ChunkHighlightCacheDimensionHandler cacheForDimension = getCacheForDimension(getActualDimension());
        if (cacheForDimension == null) return false;
        return cacheForDimension.isHighlighted(chunkPosX, chunkPosZ);
    }

    public List<HighlightAtChunkPos> getHighlightsInRegion(final int leafRegionX, final int leafRegionZ, final int level, int dimension) {
        ChunkHighlightCacheDimensionHandler cacheForDimension = getCacheForDimension(dimension);
        if (cacheForDimension == null) return Collections.emptyList();
        return cacheForDimension.getHighlightsInRegion(leafRegionX, leafRegionZ, level);
    }

    public void handleWorldChange() {
        Futures.whenAllComplete(saveAllChunks())
                .call(() -> {
                    reset();
                    initializeWorld();
                    loadChunksInActualDimension();
                    return null;
                });
    }

    public void reset() {
        this.worldCacheInitialized = false;
        this.currentWorldId = null;
        if (this.netherCache != null) this.netherCache.close();
        this.netherCache = null;
        if (this.overworldCache != null) this.overworldCache.close();
        this.overworldCache = null;
        if (this.endCache != null) this.endCache.close();
        this.endCache = null;
        if (this.database != null) this.database.close();
        this.database = null;
    }

    private List<ListenableFuture<?>> saveAllChunks() {
        return getAllCaches().stream()
                .map(ChunkHighlightCacheDimensionHandler::writeAllHighlightsToDatabase)
                .collect(Collectors.toList());
    }

    public ChunkHighlightCacheDimensionHandler getCacheForCurrentDimension() {
        switch (getActualDimension()) {
            case -1:
                return netherCache;
            case 0:
                return overworldCache;
            case 1:
                return endCache;
            default:
                throw new RuntimeException("Unknown dimension: " + getActualDimension());
        }
    }

    public ChunkHighlightCacheDimensionHandler getCacheForDimension(final int dimension) {
        switch (dimension) {
            case -1:
                return netherCache;
            case 0:
                return overworldCache;
            case 1:
                return endCache;
            default:
                throw new RuntimeException("Unknown dimension: " + dimension);
        }
    }

    private List<ChunkHighlightCacheDimensionHandler> getAllCaches() {
        return Stream.of(netherCache, overworldCache, endCache).filter(Objects::nonNull).collect(
                Collectors.toList());
    }

    public List<ChunkHighlightCacheDimensionHandler> getCachesExceptDimension(final int dimension) {
        switch (dimension) {
            case -1:
                return Stream.of(overworldCache, endCache).filter(Objects::nonNull).collect(Collectors.toList());
            case 0:
                return Stream.of(netherCache, endCache).filter(Objects::nonNull).collect(Collectors.toList());
            case 1:
                return Stream.of(netherCache, overworldCache).filter(Objects::nonNull).collect(Collectors.toList());
            default:
                throw new RuntimeException("Unknown dimension: " + dimension);
        }
    }

    private void initializeWorld() {
        try {
            final String worldId = XaeroWorldMapCore.currentSession.getMapProcessor().getCurrentWorldId();
            if (worldId == null) return;
            final int dimension = getActualDimension();
            if (dimension != 0 && dimension != -1 && dimension != 1) {
                XaeroPlus.LOGGER.error("Unexpected dimension ID: " + dimension + ". Not initializing saving cache: {}.", databaseName);
                return;
            }
            this.currentWorldId = worldId;
            this.database = new ChunkHighlightDatabase(worldId, databaseName);
            this.netherCache = new ChunkHighlightCacheDimensionHandler(-1, this.database);
            this.overworldCache = new ChunkHighlightCacheDimensionHandler(0, this.database);
            this.endCache = new ChunkHighlightCacheDimensionHandler(1, this.database);
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
        });
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
            final int mapDimension = getCurrentlyViewedDimension();
            final int mapCenterX = getGuiMapCenterRegionX(guiMap);
            final int mapCenterZ = getGuiMapCenterRegionZ(guiMap);
            final int mapSize = getGuiMapRegionSize(guiMap);
            final ChunkHighlightCacheDimensionHandler cacheForDimension = getCacheForDimension(mapDimension);
            if (cacheForDimension != null) cacheForDimension.setWindow(mapCenterX, mapCenterZ, mapSize);
            getCachesExceptDimension(mapDimension)
                .forEach(cache -> cache.setWindow(0, 0, 0));
        } else {
            final ChunkHighlightCacheDimensionHandler cacheForDimension = getCacheForDimension(getCurrentlyViewedDimension());
            if (cacheForDimension != null) cacheForDimension.setWindow(ChunkUtils.getPlayerRegionX(), ChunkUtils.getPlayerRegionZ(), defaultRegionWindowSize);
            getCachesExceptDimension(getCurrentlyViewedDimension())
                .forEach(cache -> cache.setWindow(0, 0, 0));
        }
    }

    public void removeHighlight(final int x, final int z) {
        try {
            ChunkHighlightCacheDimensionHandler cacheForCurrentDimension = getCacheForCurrentDimension();
            if (cacheForCurrentDimension != null) cacheForCurrentDimension.removeHighlight(x, z);
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error removing portal marker from saving cache", e);
        }
    }
}
