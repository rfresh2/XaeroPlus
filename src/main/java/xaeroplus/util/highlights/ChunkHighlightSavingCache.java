package xaeroplus.util.highlights;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import xaero.map.core.XaeroWorldMapCore;
import xaero.map.gui.GuiMap;
import xaeroplus.XaeroPlus;
import xaeroplus.util.ChunkUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static xaeroplus.util.ChunkUtils.getActualDimension;
import static xaeroplus.util.GuiMapHelper.*;

public class ChunkHighlightSavingCache {
    // these are initialized async
    private Optional<ChunkHighlightCacheDimensionHandler> netherCache = Optional.empty();
    private Optional<ChunkHighlightCacheDimensionHandler> overworldCache = Optional.empty();
    private Optional<ChunkHighlightCacheDimensionHandler> endCache = Optional.empty();
    private Optional<ChunkHighlightDatabase> database = Optional.empty();
    private static final int defaultRegionWindowSize = 2; // when we are only viewing the minimap
    private String currentWorldId;
    private boolean worldCacheInitialized = false;
    private final String databaseName;

    public ChunkHighlightSavingCache(final String databaseName) {
        this.databaseName = databaseName;
    }

    public boolean addHighlight(final int x, final int z) {
        try {
            getCacheForCurrentDimension()
                    .orElseThrow(() -> new RuntimeException("Didn't find cache for current dimension"))
                    .addHighlight(x, z);
            return true;
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error adding highlight to saving cache: {}", databaseName, e);
            return false;
        }
    }

    public void addHighlight(final int x, final int z, final long foundTime, final int dimension) {
        getCacheForDimension(dimension)
                .orElseThrow(() -> new RuntimeException("Dimension not found: " + dimension))
                .addHighlight(x, z, foundTime);
    }

    public boolean isHighlighted(final int chunkPosX, final int chunkPosZ, final int dimensionId) {
        return getCacheForDimension(dimensionId)
                .map(c -> c.isHighlighted(chunkPosX, chunkPosZ))
                .orElse(false);
    }

    public boolean isHighlighted(final int chunkPosX, final int chunkPosZ) {
        return getCacheForDimension(ChunkUtils.getActualDimension())
                .map(c -> c.isHighlighted(chunkPosX, chunkPosZ))
                .orElse(false);
    }

    public List<HighlightAtChunkPos> getHighlightsInRegion(final int leafRegionX, final int leafRegionZ, final int level, int dimension) {
        return getCacheForDimension(dimension)
                .map(c -> c.getHighlightsInRegion(leafRegionX, leafRegionZ, level))
                .orElse(Collections.emptyList());
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
        this.netherCache.ifPresent(ChunkHighlightCacheDimensionHandler::close);
        this.netherCache = Optional.empty();
        this.overworldCache.ifPresent(ChunkHighlightCacheDimensionHandler::close);
        this.overworldCache = Optional.empty();
        this.endCache.ifPresent(ChunkHighlightCacheDimensionHandler::close);
        this.endCache = Optional.empty();
        this.database.ifPresent(ChunkHighlightDatabase::close);
        this.database = Optional.empty();
    }

    private List<ListenableFuture<?>> saveAllChunks() {
        return getAllCaches().stream()
                .map(ChunkHighlightCacheDimensionHandler::writeAllHighlightsToDatabase)
                .collect(Collectors.toList());
    }

    public Optional<ChunkHighlightCacheDimensionHandler> getCacheForCurrentDimension() {
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

    public Optional<ChunkHighlightCacheDimensionHandler> getCacheForDimension(final int dimension) {
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
        return Stream.of(netherCache, overworldCache, endCache).filter(Optional::isPresent).map(Optional::get).collect(
                Collectors.toList());
    }

    public List<ChunkHighlightCacheDimensionHandler> getCachesExceptDimension(final int dimension) {
        switch (dimension) {
            case -1:
                return Stream.of(overworldCache, endCache).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
            case 0:
                return Stream.of(netherCache, endCache).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
            case 1:
                return Stream.of(netherCache, overworldCache).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
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
            final ChunkHighlightDatabase db = new ChunkHighlightDatabase(worldId, databaseName);
            this.database = Optional.of(db);
            this.netherCache = Optional.of(new ChunkHighlightCacheDimensionHandler(-1, db));
            this.overworldCache = Optional.of(new ChunkHighlightCacheDimensionHandler(0, db));
            this.endCache = Optional.of(new ChunkHighlightCacheDimensionHandler(1, db));
            this.worldCacheInitialized = true;
            loadChunksInActualDimension();
        } catch (final Exception e) {
            // expected on game launch
        }
    }

    private void loadChunksInActualDimension() {
        getCacheForCurrentDimension().ifPresent(
                c -> c.setWindow(ChunkUtils.actualPlayerRegionX(), ChunkUtils.actualPlayerRegionZ(), defaultRegionWindowSize));
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
            getCacheForDimension(mapDimension).ifPresent(c -> c.setWindow(mapCenterX, mapCenterZ, mapSize));
            getCachesExceptDimension(mapDimension).forEach(cache -> cache.setWindow(0, 0, 0));
        } else {
            getCacheForDimension(getCurrentlyViewedDimension()).ifPresent(c -> c.setWindow(ChunkUtils.getPlayerRegionX(), ChunkUtils.getPlayerRegionZ(), defaultRegionWindowSize));
            getCachesExceptDimension(getCurrentlyViewedDimension()).forEach(cache -> cache.setWindow(0, 0, 0));
        }
    }

    public void removeHighlight(final int x, final int z) {
        try {
            getCacheForCurrentDimension().ifPresent(c -> c.removeHighlight(x, z));
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error removing portal marker from saving cache", e);
        }
    }
}
