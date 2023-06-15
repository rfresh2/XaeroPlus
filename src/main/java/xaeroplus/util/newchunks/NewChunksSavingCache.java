package xaeroplus.util.newchunks;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import xaero.map.core.XaeroWorldMapCore;
import xaero.map.gui.GuiMap;
import xaeroplus.XaeroPlus;
import xaeroplus.util.ChunkUtils;
import xaeroplus.util.HighlightAtChunkPos;
import xaeroplus.util.Shared;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.minecraft.world.World.*;
import static xaeroplus.util.ChunkUtils.getMCDimension;
import static xaeroplus.util.GuiMapHelper.*;

public class NewChunksSavingCache implements NewChunksCache {

    // these are initialized async
    private Optional<NewChunksSavingCacheDimensionHandler> netherCache = Optional.empty();
    private Optional<NewChunksSavingCacheDimensionHandler> overworldCache = Optional.empty();
    private Optional<NewChunksSavingCacheDimensionHandler> endCache = Optional.empty();
    private Optional<NewChunksDatabase> newChunksDatabase = Optional.empty();
    private static final int defaultRegionWindowSize = 2; // when we are only viewing the minimap
    private String currentWorldId;
    private boolean worldCacheInitialized = false;

    @Override
    public void addNewChunk(final int x, final int z) {
        try {
            getCacheForCurrentDimension().ifPresent(c -> c.addNewChunk(x, z));
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error adding new chunk to saving cache", e);
        }
    }

    public void addNewChunk(final int x, final int z, final long foundTime, final RegistryKey<World> dimension) {
        getCacheForDimension(dimension)
                .orElseThrow(() -> new RuntimeException("Dimension not found: " + dimension))
                .addNewChunk(x, z, foundTime);
    }

    @Override
    public boolean isNewChunk(final int chunkPosX, final int chunkPosZ, final RegistryKey<World> dimensionId) {
        return getCacheForDimension(dimensionId)
                .map(c -> c.isNewChunk(chunkPosX, chunkPosZ))
                .orElse(false);
    }

    @Override
    public List<HighlightAtChunkPos> getNewChunksInRegion(final int leafRegionX, final int leafRegionZ, final int level, final RegistryKey<World> dimension) {
        return getCacheForDimension(dimension)
                .map(c -> c.getNewChunksInRegion(leafRegionX, leafRegionZ, level))
                .orElse(Collections.emptyList());
    }

    @Override
    public void handleWorldChange() {
        Futures.whenAllComplete(saveAllChunks())
                        .call(() -> {
                            reset();
                            initializeWorld();
                            loadChunksInCurrentDimension();
                            return null;
                        }, Shared.cacheRefreshExecutorService);
    }

    int tickCounter = 0;
    @Override
    public void handleTick() {
        if (!worldCacheInitialized) return;
        // limit so we don't overflow
        if (tickCounter > 2400) tickCounter = 0;
        if (tickCounter++ % 30 != 0) { // run once every 1.5 seconds
            return;
        }
        // autosave current window every 60 seconds
        if (tickCounter % 1200 == 0) {
            getAllCaches().forEach(NewChunksSavingCacheDimensionHandler::writeAllChunksToDatabase);
            return;
        }

        Optional<GuiMap> guiMapOptional = getGuiMap();
        if (guiMapOptional.isPresent()) {
            final GuiMap guiMap = guiMapOptional.get();
            final RegistryKey<World> mapDimension = getCurrentlyViewedDimension();
            final int mapCenterX = getGuiMapCenterRegionX(guiMap);
            final int mapCenterZ = getGuiMapCenterRegionZ(guiMap);
            final int mapSize = getGuiMapRegionSize(guiMap);
            getCacheForDimension(mapDimension).ifPresent(c -> c.setWindow(mapCenterX, mapCenterZ, mapSize));
            getCachesExceptDimension(mapDimension).forEach(cache -> cache.setWindow(0, 0, 0));
        } else {
            getCachesExceptDimension(getCurrentlyViewedDimension()).forEach(cache -> cache.setWindow(0, 0, 0));
            getCacheForCurrentDimension().ifPresent(c -> c.setWindow(ChunkUtils.currentPlayerRegionX(), ChunkUtils.currentPlayerRegionZ(), defaultRegionWindowSize));
        }
    }

    @Override
    public void onEnable() {
        initializeWorld();
    }

    @Override
    public void onDisable() {
        Futures.whenAllComplete(saveAllChunks()).call(() -> {
            reset();
            return null;
        }, Shared.cacheRefreshExecutorService);
    }

    @Override
    public Long2LongOpenHashMap getNewChunksState() {
        return getCacheForCurrentDimension().map(NewChunksSavingCacheDimensionHandler::getNewChunksState).orElse(new Long2LongOpenHashMap());
    }

    @Override
    public void loadPreviousState(final Long2LongOpenHashMap state) {
        getCacheForCurrentDimension().ifPresent(c -> c.loadPreviousState(state));
    }

    public void reset() {
        this.worldCacheInitialized = false;
        this.currentWorldId = null;
        this.netherCache.ifPresent(NewChunksSavingCacheDimensionHandler::close);
        this.netherCache = Optional.empty();
        this.overworldCache.ifPresent(NewChunksSavingCacheDimensionHandler::close);
        this.overworldCache = Optional.empty();
        this.endCache.ifPresent(NewChunksSavingCacheDimensionHandler::close);
        this.endCache = Optional.empty();
        this.newChunksDatabase.ifPresent(NewChunksDatabase::close);
        this.newChunksDatabase = Optional.empty();
    }

    private List<ListenableFuture<?>> saveAllChunks() {
        return getAllCaches().stream()
                .map(NewChunksSavingCacheDimensionHandler::writeAllChunksToDatabase)
                .collect(Collectors.toList());
    }

    public Optional<NewChunksSavingCacheDimensionHandler> getCacheForCurrentDimension() {
        RegistryKey<World> mcDimension = getMCDimension();
        if (mcDimension.equals(NETHER)) {
            return netherCache;
        } else if (mcDimension.equals(OVERWORLD)) {
            return overworldCache;
        } else if (mcDimension.equals(END)) {
            return endCache;
        }
        throw new RuntimeException("Unknown dimension: " + getMCDimension());
    }

    public Optional<NewChunksSavingCacheDimensionHandler> getCacheForDimension(final RegistryKey<World> dimension) {
        if (dimension.equals(NETHER)) {
            return netherCache;
        } else if (dimension.equals(OVERWORLD)) {
            return overworldCache;
        } else if (dimension.equals(END)) {
            return endCache;
        }
        throw new RuntimeException("Unknown dimension: " + dimension);
    }

    private List<NewChunksSavingCacheDimensionHandler> getAllCaches() {
        return Stream.of(netherCache, overworldCache, endCache).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
    }

    public List<NewChunksSavingCacheDimensionHandler> getCachesExceptDimension(final RegistryKey<World> dimension) {
        if (dimension.equals(NETHER)) {
            return Stream.of(overworldCache,
                             endCache).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
        } else if (dimension.equals(OVERWORLD)) {
            return Stream.of(netherCache,
                             endCache).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
        } else if (dimension.equals(END)) {
            return Stream.of(netherCache, overworldCache).filter(Optional::isPresent).map(Optional::get).collect(
                    Collectors.toList());
        }
        throw new RuntimeException("Unknown dimension: " + dimension);
    }

    private void initializeWorld() {
        try {
            final String worldId = XaeroWorldMapCore.currentSession.getMapProcessor().getCurrentWorldId();
            final String mwId = XaeroWorldMapCore.currentSession.getMapProcessor().getCurrentMWId();
            if (worldId == null || mwId == null) return;
            final RegistryKey<World> dimension = getMCDimension();
            if (dimension != OVERWORLD && dimension != NETHER && dimension != END) {
                XaeroPlus.LOGGER.error("Unexpected dimension ID: " + dimension + ". Disable Save/Load NewChunks to Disk to restore functionality.");
                return;
            }

            this.currentWorldId = worldId;
            final NewChunksDatabase db = new NewChunksDatabase(worldId);
            this.newChunksDatabase = Optional.of(db);
            this.netherCache = Optional.of(new NewChunksSavingCacheDimensionHandler(-1, db));
            this.overworldCache = Optional.of(new NewChunksSavingCacheDimensionHandler(0, db));
            this.endCache = Optional.of(new NewChunksSavingCacheDimensionHandler(1, db));
            this.worldCacheInitialized = true;
            NewChunksV1Converter.convert(this, worldId, mwId);
            loadChunksInCurrentDimension();
        } catch (final Exception e) {
            // expected on game launch
        }
    }

    private void loadChunksInCurrentDimension() {
        getCacheForCurrentDimension()
                .ifPresent(c ->
                        c.setWindow(ChunkUtils.currentPlayerRegionX(), ChunkUtils.currentPlayerRegionZ(), defaultRegionWindowSize));
    }
}
