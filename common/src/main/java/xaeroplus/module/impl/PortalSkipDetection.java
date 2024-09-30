package xaeroplus.module.impl;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.lenni0451.lambdaevents.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaero.map.MapProcessor;
import xaero.map.WorldMapSession;
import xaero.map.core.XaeroWorldMapCore;
import xaero.map.gui.GuiMap;
import xaero.map.region.MapRegion;
import xaero.map.region.MapTileChunk;
import xaeroplus.Globals;
import xaeroplus.XaeroPlus;
import xaeroplus.event.ClientTickEvent;
import xaeroplus.event.XaeroWorldChangeEvent;
import xaeroplus.feature.extensions.SeenChunksTrackingMapTileChunk;
import xaeroplus.feature.render.highlights.ChunkHighlightLocalCache;
import xaeroplus.module.Module;
import xaeroplus.module.ModuleManager;
import xaeroplus.settings.Settings;
import xaeroplus.util.ChunkUtils;
import xaeroplus.util.ColorHelper;

import java.util.Optional;
import java.util.concurrent.Future;

import static xaeroplus.util.ChunkUtils.*;
import static xaeroplus.util.GuiMapHelper.*;

public class PortalSkipDetection extends Module {
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
    private int portalRadius = 15;
    private boolean oldChunksInverse = false;
    private boolean newChunks = false;
    private OldChunks oldChunksModule;
    private PaletteNewChunks newChunksModule;

    @EventHandler
    public void onClientTickEvent(final ClientTickEvent.Post event) {
        if (!worldCacheInitialized
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

    @EventHandler
    public void onXaeroWorldChangeEvent(final XaeroWorldChangeEvent event) {
        reset();
        initializeWorld();
    }

    @Override
    public void onEnable() {
        Globals.drawManager.registerChunkHighlightProvider(
            this.getClass(),
            this::getHighlightsSnapshot,
            this::getPortalSkipChunksColor);
        reset();
        initializeWorld();
        this.newChunksModule = ModuleManager.getModule(PaletteNewChunks.class);
        this.oldChunksModule = ModuleManager.getModule(OldChunks.class);
    }

    @Override
    public void onDisable() {
        reset();
        Globals.drawManager.unregisterChunkHighlightProvider(this.getClass());
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
        if (Minecraft.getInstance().level == null) return;
        portalSkipDetectionSearchFuture = Globals.moduleExecutorService.get().submit(this::searchForPortalSkipChunks);
    }

    private final LongOpenHashSet portalDetectionSearchChunksBuf = new LongOpenHashSet();
    private final Long2LongOpenHashMap portalAreaChunksBuf = new Long2LongOpenHashMap();
    private final LongOpenHashSet portalChunkTempSetBuf = new LongOpenHashSet();

    private void searchForPortalSkipChunks() {
        try {
            final int windowRegionX = this.windowRegionX;
            final int windowRegionZ = this.windowRegionZ;
            final int windowRegionSize = this.windowRegionSize;
            final ResourceKey<Level> currentlyViewedDimension = Globals.getCurrentDimensionId();
            portalDetectionSearchChunksBuf.clear();
            for (int regionX = windowRegionX - windowRegionSize; regionX <= windowRegionX + windowRegionSize; regionX++) {
                final int baseChunkCoordX = ChunkUtils.regionCoordToChunkCoord(regionX);
                for (int regionZ = windowRegionZ - windowRegionSize; regionZ <= windowRegionZ + windowRegionSize; regionZ++) {
                    final int baseChunkCoordZ = ChunkUtils.regionCoordToChunkCoord(regionZ);
                    for (int chunkX = 0; chunkX < 32; chunkX++) {
                        for (int chunkZ = 0; chunkZ < 32; chunkZ++) {
                            final int chunkPosX = baseChunkCoordX + chunkX;
                            final int chunkPosZ = baseChunkCoordZ + chunkZ;
                            if (isChunkSeen(chunkPosX, chunkPosZ, currentlyViewedDimension) && !isNewishChunk(chunkPosX, chunkPosZ, currentlyViewedDimension)) {
                                portalDetectionSearchChunksBuf.add(ChunkUtils.chunkPosToLong(chunkPosX, chunkPosZ));
                            }
                        }
                    }
                }
            }
            portalAreaChunksBuf.clear();
            for (final long chunkPos : portalDetectionSearchChunksBuf) {
                boolean allSeen = true;
                portalChunkTempSetBuf.clear();
                for (int xOffset = 0; xOffset < portalRadius; xOffset++) {
                    for (int zOffset = 0; zOffset < portalRadius; zOffset++) {
                        final long currentChunkPos = ChunkUtils.chunkPosToLong(ChunkUtils.longToChunkX(chunkPos) + xOffset, ChunkUtils.longToChunkZ(chunkPos) + zOffset);
                        portalChunkTempSetBuf.add(currentChunkPos);
                        if (!portalDetectionSearchChunksBuf.contains(currentChunkPos)) {
                            allSeen = false;
                            portalChunkTempSetBuf.clear();
                            break;
                        }
                    }
                    if (!allSeen) {
                        break;
                    }
                }
                if (allSeen) portalChunkTempSetBuf.forEach(c -> portalAreaChunksBuf.put(c, 0));
            }
            cache.replaceState(portalAreaChunksBuf);
        } catch (final Exception e) {
            XaeroPlus.LOGGER.debug("Error searching for portal skip chunks", e);
        }
    }

    private boolean isNewishChunk(final int chunkPosX, final int chunkPosZ, final ResourceKey<Level> currentlyViewedDimension) {
        if (newChunks && oldChunksInverse) {
            return isNewChunk(chunkPosX, chunkPosZ, currentlyViewedDimension) || isOldChunksInverse(chunkPosX, chunkPosZ, currentlyViewedDimension);
        } else if (newChunks) {
            return isNewChunk(chunkPosX, chunkPosZ, currentlyViewedDimension);
        } else if (oldChunksInverse) {
            return isOldChunksInverse(chunkPosX, chunkPosZ, currentlyViewedDimension);
        } else {
            return false;
        }
    }

    private boolean isNewChunk(final int chunkPosX, final int chunkPosZ, final ResourceKey<Level> currentlyViewedDimension) {
        if (Settings.REGISTRY.paletteNewChunksSaveLoadToDisk.get() && newChunksModule != null)
            return newChunksModule.isNewChunk(chunkPosX, chunkPosZ, currentlyViewedDimension);
        else
            return false;
    }

    private boolean isOldChunksInverse(final int chunkPosX, final int chunkPosZ, final ResourceKey<Level> currentlyViewedDimension) {
        if (Settings.REGISTRY.oldChunksEnabledSetting.get() && oldChunksModule != null)
            return oldChunksModule.isOldChunkInverse(chunkPosX, chunkPosZ, currentlyViewedDimension);
        else
            return false;
    }

    private boolean isChunkSeen(final int chunkPosX, final int chunkPosZ, final ResourceKey<Level> currentlyViewedDimension) {
        final WorldMapSession currentSession = XaeroWorldMapCore.currentSession;
        if (currentSession == null) return false;
        final MapProcessor mapProcessor = currentSession.getMapProcessor();
        if (mapProcessor == null) return false;
        final MapRegion mapRegion = mapProcessor.getLeafMapRegion(
            mapProcessor.getCurrentCaveLayer(),
            ChunkUtils.chunkCoordToMapRegionCoord(chunkPosX),
            ChunkUtils.chunkCoordToMapRegionCoord(chunkPosZ),
            false);
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
        portalSkipChunksColor = ColorHelper.getColorWithAlpha(color, Settings.REGISTRY.portalSkipDetectionAlphaSetting.getAsInt());
    }

    public void setAlpha(final double a) {
        portalSkipChunksColor = ColorHelper.getColorWithAlpha(portalSkipChunksColor, (int) a);
    }

    public boolean isPortalSkipChunk(final int chunkPosX, final int chunkPosZ, final ResourceKey<Level> dimension) {
        return isPortalSkipChunk(chunkPosToLong(chunkPosX, chunkPosZ));
    }

    public LongList getHighlightsSnapshot(final int windowRegionX, final int windowRegionZ, final int windowRegionSize, final ResourceKey<Level> dimension) {
        return cache.getHighlightsSnapshot(dimension);
    }

    public boolean isPortalSkipChunk(final long chunkPos) {
        return cache.isHighlighted(chunkPos);
    }

    public void setSearchDelayTicks(final double delay) {
        searchDelayTicks = (int) delay;
    }

    public void setOldChunksInverse(final boolean b) {
        this.oldChunksInverse = b;
    }

    public void setNewChunks(final boolean b) {
        this.newChunks = b;
    }

    public void setPortalRadius(final double b) {
        this.portalRadius = (int) b;
    }
}
