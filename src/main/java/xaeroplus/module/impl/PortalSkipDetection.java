package xaeroplus.module.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.util.math.ChunkPos;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
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
import xaeroplus.util.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import static xaeroplus.util.ChunkUtils.*;
import static xaeroplus.util.GuiMapHelper.*;

@Module.ModuleInfo()
public class PortalSkipDetection extends Module {
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Future<?> portalSkipDetectionSearchFuture = null;
    private int portalSkipChunksColor = ColorHelper.getColor(255, 255, 255, 100);
    private final LongOpenHashSet portalSkipChunks = new LongOpenHashSet();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Cache<RegionRenderPos, List<HighlightAtChunkPos>> regionRenderCache = CacheBuilder.newBuilder()
            .expireAfterWrite(500, TimeUnit.MILLISECONDS)
            .build();
    private int windowRegionX = 0;
    private int windowRegionZ = 0;
    private int windowRegionSize = 0;
    private static final int defaultRegionWindowSize = 2; // when we are only viewing the minimap
    private boolean worldCacheInitialized = false;
    private int searchDelayTicks = 0;
    private int tickCounter = 10000;

    @SubscribeEvent
    public void onClientTickEvent(final TickEvent.ClientTickEvent event) {
        if (!worldCacheInitialized) return;
        if (event.phase == TickEvent.Phase.END) {
            if (portalSkipDetectionSearchFuture == null || portalSkipDetectionSearchFuture.isDone()) {
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
                        setWindow(ChunkUtils.currentPlayerRegionX(), ChunkUtils.currentPlayerRegionZ(), defaultRegionWindowSize);
                    }
                }
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
            final String mwId = XaeroWorldMapCore.currentSession.getMapProcessor().getCurrentMWId();
            if (worldId == null || mwId == null) return;
            worldCacheInitialized = true;
        } catch (final Exception e) {
            // expected on game launch
        }
    }

    private void reset() {
        try {
            lock.writeLock().lock();
            portalSkipChunks.clear();
        } finally {
            lock.writeLock().unlock();
        }
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
            final HashSet<ChunkPos> portalDetectionSearchChunks = new HashSet<>();
            for (int regionX = windowRegionX - windowRegionSize; regionX <= windowRegionX + windowRegionSize; regionX++) {
                final int baseChunkCoordX = ChunkUtils.regionCoordToChunkCoord(regionX);
                for (int regionZ = windowRegionZ - windowRegionSize; regionZ <= windowRegionZ + windowRegionSize; regionZ++) {
                    final int baseChunkCoordZ = ChunkUtils.regionCoordToChunkCoord(regionZ);
                    for (int chunkX = 0; chunkX < 32; chunkX++) {
                        for (int chunkZ = 0; chunkZ < 32; chunkZ++) {
                            final int chunkPosX = baseChunkCoordX + chunkX;
                            final int chunkPosZ = baseChunkCoordZ + chunkZ;
                            if (isChunkSeen(chunkPosX, chunkPosZ, currentlyViewedDimension)) {
                                if (!isNewChunk(chunkPosX, chunkPosZ, currentlyViewedDimension)) {
                                    portalDetectionSearchChunks.add(new ChunkPos(chunkPosX, chunkPosZ));
                                }
                            }
                        }
                    }
                }
            }
            final HashSet<ChunkPos> portalAreaChunks = new HashSet<>();
            for (final ChunkPos chunkPos : portalDetectionSearchChunks) {
                boolean allSeen = true;
                final HashSet<ChunkPos> portalChunkTempSet = new HashSet<>();
                for (int xOffset = 0; xOffset < 15; xOffset++) {
                    for (int zOffset = 0; zOffset < 15; zOffset++) {
                        final ChunkPos currentChunkPos = new ChunkPos(chunkPos.x + xOffset, chunkPos.z + zOffset);
                        portalChunkTempSet.add(currentChunkPos);
                        if (!portalDetectionSearchChunks.contains(currentChunkPos)) {
                            allSeen = false;
                            portalChunkTempSet.clear();
                            break;
                        }
                    }
                    if (!allSeen) {
                        portalChunkTempSet.clear();
                        break;
                    }
                }
                if (allSeen) portalAreaChunks.addAll(portalChunkTempSet);
            }
            final List<Long> chunks = portalAreaChunks.stream().map(chunkPos -> chunkPosToLong(chunkPos.x, chunkPos.z)).collect(Collectors.toList());
            try {
                lock.writeLock().lock();
                this.portalSkipChunks.clear();
                this.portalSkipChunks.addAll(chunks);
            } finally {
                lock.writeLock().unlock();
            }
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error searching for portal skip chunks", e);
        }
    }

    private boolean isNewChunk(final int chunkPosX, final int chunkPosZ, final int currentlyViewedDimension) {
        return ModuleManager.getModule(NewChunks.class).isNewChunk(chunkPosX, chunkPosZ, currentlyViewedDimension);
    }

    private boolean isChunkSeen(final int chunkPosX, final int chunkPosZ, final int currentlyViewedDimension) {
        //5100,7578
        //318,473
        final WorldMapSession currentSession = XaeroWorldMapCore.currentSession;
        if (currentSession == null) return false;
        final CustomDimensionMapProcessor mapProcessor = (CustomDimensionMapProcessor) currentSession.getMapProcessor();
        if (mapProcessor == null) return false;
        final MapRegion mapRegion = mapProcessor.getMapRegionCustomDimension(
                ChunkUtils.chunkCoordToMapRegionCoord(chunkPosX),
                ChunkUtils.chunkCoordToMapRegionCoord(chunkPosZ),
                false,
                currentlyViewedDimension);
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
        try {
            return regionRenderCache.get(new RegionRenderPos(leafRegionX, leafRegionZ, level), loadHighlightChunksAtRegion(leafRegionX, leafRegionZ, level, this::isPortalSkipChunk));
        } catch (ExecutionException e) {
            XaeroPlus.LOGGER.error("Error loading portal skip chunks", e);
        }
        return Collections.emptyList();
    }

    public boolean isPortalSkipChunk(final int chunkPosX, final int chunkPosZ) {
        return isPortalSkipChunk(chunkPosToLong(chunkPosX, chunkPosZ));
    }

    public boolean isPortalSkipChunk(final long chunkPos) {
        try {
            if (lock.readLock().tryLock(1, TimeUnit.SECONDS)) {
                boolean containsKey = portalSkipChunks.contains(chunkPos);
                lock.readLock().unlock();
                return containsKey;
            }
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error checking if chunk is portal skip chunk", e);
        }
        return false;
    }

    public void setSearchDelayTicks(final float delay) {
        searchDelayTicks = (int) delay;
    }
}
