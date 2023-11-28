package xaeroplus.module.impl;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import xaero.map.MapProcessor;
import xaero.map.WorldMapSession;
import xaero.map.core.XaeroWorldMapCore;
import xaero.map.gui.GuiMap;
import xaero.map.region.MapRegion;
import xaero.map.region.MapTileChunk;
import xaeroplus.XaeroPlus;
import xaeroplus.event.XaeroWorldChangeEvent;
import xaeroplus.module.Module;
import xaeroplus.module.ModuleManager;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.util.ChunkUtils;
import xaeroplus.util.ColorHelper;
import xaeroplus.util.SeenChunksTrackingMapTileChunk;
import xaeroplus.util.highlights.ChunkHighlightLocalCache;
import xaeroplus.util.highlights.HighlightAtChunkPos;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static xaeroplus.util.ChunkUtils.*;
import static xaeroplus.util.GuiMapHelper.*;

@Module.ModuleInfo()
public class PortalSkipDetection extends Module {
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Future<?> portalSkipDetectionSearchFuture = null;
    private int portalSkipChunksColor = ColorHelper.getColor(255, 255, 255, 100);
    private final ChunkHighlightLocalCache cache = new ChunkHighlightLocalCache();
    private int windowRegionX = 0;
    private int windowRegionZ = 0;
    private int windowRegionSize = 0;
    private static final int defaultRegionWindowSize = 2; // when we are only viewing the minimap
    private boolean worldCacheInitialized = false;
    private int searchDelayTicks = 0;
    private int tickCounter = 10000;

    @SubscribeEvent
    public void onClientTickEvent(final TickEvent.ClientTickEvent event) {
        if (!worldCacheInitialized
                || event.phase != TickEvent.Phase.END
                || portalSkipDetectionSearchFuture != null
                && !portalSkipDetectionSearchFuture.isDone()) return;
        tickCounter++;
        if (tickCounter >= searchDelayTicks) {
            tickCounter = 0;
            Optional<GuiMap> guiMapOptional = getGuiMap();
            if (guiMapOptional.isPresent()) {
                final GuiMap guiMap = guiMapOptional.get();
                final int mapCenterX = getGuiMapCenterRegionX(guiMap);
                final int mapCenterZ = getGuiMapCenterRegionZ(guiMap);
                final int mapSize = getGuiMapRegionSize(guiMap);
                setWindow(mapCenterX, mapCenterZ, mapSize);
            } else {
                setWindow(ChunkUtils.getPlayerRegionX(), ChunkUtils.getPlayerRegionZ(), defaultRegionWindowSize);
            }
        }
    }

    @SubscribeEvent
    public void onXaeroWorldChangeEvent(final XaeroWorldChangeEvent event) {
        reset();
        initializeWorld();
    }

    @Override
    public void onEnable() {
        reset();
        initializeWorld();
    }

    @Override
    public void onDisable() {
        reset();
    }

    private void initializeWorld() {
        try {
            final String worldId = XaeroWorldMapCore.currentSession.getMapProcessor().getCurrentWorldId();
            if (worldId == null) return;
            worldCacheInitialized = true;
        } catch (final Exception e) {
            // expected on game launch
        }
    }

    private void reset() {
        cache.reset();
        final Future<?> future = portalSkipDetectionSearchFuture;
        if (future != null && !future.isDone()) {
            future.cancel(true);
        }
    }

    public void setWindow(int regionX, int regionZ, int regionSize) {
        windowRegionX = regionX;
        windowRegionZ = regionZ;
        windowRegionSize = regionSize;
        portalSkipDetectionSearchFuture = executorService.submit(this::searchForPortalSkipChunks);
    }

    private void searchForPortalSkipChunks() {
        try {
            final int windowRegionX = this.windowRegionX;
            final int windowRegionZ = this.windowRegionZ;
            final int windowRegionSize = this.windowRegionSize;
            final int currentlyViewedDimension = getCurrentlyViewedDimension();
            final LongOpenHashSet portalDetectionSearchChunks = new LongOpenHashSet();
            for (int regionX = windowRegionX - windowRegionSize; regionX <= windowRegionX + windowRegionSize; regionX++) {
                final int baseChunkCoordX = ChunkUtils.regionCoordToChunkCoord(regionX);
                for (int regionZ = windowRegionZ - windowRegionSize; regionZ <= windowRegionZ + windowRegionSize; regionZ++) {
                    final int baseChunkCoordZ = ChunkUtils.regionCoordToChunkCoord(regionZ);
                    for (int chunkX = 0; chunkX < 32; chunkX++) {
                        for (int chunkZ = 0; chunkZ < 32; chunkZ++) {
                            final int chunkPosX = baseChunkCoordX + chunkX;
                            final int chunkPosZ = baseChunkCoordZ + chunkZ;
                            if (isChunkSeen(chunkPosX, chunkPosZ, currentlyViewedDimension) && !isNewChunk(chunkPosX, chunkPosZ, currentlyViewedDimension)) {
                                portalDetectionSearchChunks.add(ChunkUtils.chunkPosToLong(chunkPosX, chunkPosZ));
                            }
                        }
                    }
                }
            }
            final Long2LongOpenHashMap portalAreaChunks = new Long2LongOpenHashMap();
            for (final long chunkPos : portalDetectionSearchChunks) {
                boolean allSeen = true;
                final LongOpenHashSet portalChunkTempSet = new LongOpenHashSet();
                for (int xOffset = 0; xOffset < 15; xOffset++) {
                    for (int zOffset = 0; zOffset < 15; zOffset++) {
                        final long currentChunkPos = ChunkUtils.chunkPosToLong(ChunkUtils.longToChunkX(chunkPos) + xOffset, ChunkUtils.longToChunkZ(chunkPos) + zOffset);
                        portalChunkTempSet.add(currentChunkPos);
                        if (!portalDetectionSearchChunks.contains(currentChunkPos)) {
                            allSeen = false;
                            portalChunkTempSet.clear();
                            break;
                        }
                    }
                    if (!allSeen) {
                        break;
                    }
                }
                if (allSeen) portalChunkTempSet.forEach(c -> portalAreaChunks.put(c, 0));
            }
            cache.replaceState(portalAreaChunks);
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error searching for portal skip chunks", e);
        }
    }

    private boolean isNewChunk(final int chunkPosX, final int chunkPosZ, final int currentlyViewedDimension) {
        return ModuleManager.getModule(NewChunks.class).isNewChunk(chunkPosX, chunkPosZ, currentlyViewedDimension);
    }

    private boolean isChunkSeen(final int chunkPosX, final int chunkPosZ, final int currentlyViewedDimension) {
        final WorldMapSession currentSession = XaeroWorldMapCore.currentSession;
        if (currentSession == null) return false;
        final MapProcessor mapProcessor = currentSession.getMapProcessor();
        if (mapProcessor == null) return false;
        final MapRegion mapRegion = mapProcessor.getMapRegion(
                ChunkUtils.chunkCoordToMapRegionCoord(chunkPosX),
                ChunkUtils.chunkCoordToMapRegionCoord(chunkPosZ),
                false
        );
        if (mapRegion == null) return false;
        final MapTileChunk mapChunk = mapRegion.getChunk(chunkCoordToMapTileChunkCoordLocal(chunkPosX), chunkCoordToMapTileChunkCoordLocal(chunkPosZ));
        if (mapChunk == null) return false;
        // todo: known issue: if the worldmap is serving from the texture cache (e.g. at low zooms), the tile may not be loaded and marked seen
        //  we could try to examine the texture for a black color at a particular pixel, but that's a bit hacky
        //  alternatively we could try to load the tile, but that could cause performance issues
        return ((SeenChunksTrackingMapTileChunk) mapChunk).getSeenTiles()[chunkCoordToMapTileCoordLocal(chunkPosX)][chunkCoordToMapTileCoordLocal(chunkPosZ)];
    }

    public int getPortalSkipChunksColor() {
        return portalSkipChunksColor;
    }

    public void setRgbColor(final int color) {
        portalSkipChunksColor = ColorHelper.getColorWithAlpha(color, (int) XaeroPlusSettingRegistry.portalSkipDetectionAlphaSetting.getValue());
    }

    public void setAlpha(final float a) {
        portalSkipChunksColor = ColorHelper.getColorWithAlpha(portalSkipChunksColor, (int) a);
    }

    public List<HighlightAtChunkPos> getPortalSkipChunksInRegion(
            final int leafRegionX, final int leafRegionZ,
            final int level) {
        return cache.getHighlightsInRegion(leafRegionX, leafRegionZ, level);
    }

    public boolean isPortalSkipChunk(final int chunkPosX, final int chunkPosZ) {
        return isPortalSkipChunk(chunkPosToLong(chunkPosX, chunkPosZ));
    }

    public boolean isPortalSkipChunk(final long chunkPos) {
        return cache.isHighlighted(chunkPos);
    }

    public void setSearchDelayTicks(final float delay) {
        searchDelayTicks = (int) delay;
    }
}
