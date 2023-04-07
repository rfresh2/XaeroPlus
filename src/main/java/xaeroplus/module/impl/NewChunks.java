package xaeroplus.module.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.network.play.server.SPacketChunkData;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import xaero.map.core.XaeroWorldMapCore;
import xaero.map.gui.GuiMap;
import xaeroplus.XaeroPlus;
import xaeroplus.event.PacketReceivedEvent;
import xaeroplus.event.XaeroWorldChangeEvent;
import xaeroplus.module.Module;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.util.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static xaeroplus.util.ColorHelper.getColor;
import static xaeroplus.util.GuiMapHelper.*;

@Module.ModuleInfo()
public class NewChunks extends Module {
    // idea: store a "window" of x and z range around the player or current map center
    //  we want to constantly adjust that window and load in new chunks as we move around
    //  and save chunks that are no longer in the window
    //  we should also have a map for each dimension so we can have multiple dimensions open concurrently
    //  the active dimension to write to we can determine based on the player's actual dimension
    //  that one always needs to be opened and available for writes

    private Optional<NewChunksLocalCache> netherCache = Optional.empty();
    private Optional<NewChunksLocalCache> overworldCache = Optional.empty();
    private Optional<NewChunksLocalCache> endCache = Optional.empty();
    private Optional<NewChunksDatabase> newChunksDatabase = Optional.empty();

    private boolean worldCacheInitialized = false;

    // only used when we aren't writing chunks to disk
    // todo: create an interface for chunk storage and in-memory vs disk be two implementations
//    private final Long2LongOpenHashMap chunks = new Long2LongOpenHashMap();
    private int newChunksColor = getColor(255, 0, 0, 100);
    // somewhat arbitrary number but should be sufficient
    private static final int maxNumber = 5000;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    // this is an lz4 compressed JSON file
    // I've added v1 as a suffix if we ever need to change file formats and want to convert these without data loss
    // todo: convert these into the sqlite db on startup, then move file to a backup location
    private static final String NEWCHUNKS_FILE_NAME = "XaeroPlusNewChunksV1.data";
    private String currentWorldId;
    private static final int defaultRegionWindowSize = 3;


    // todo: handle save load on custom dimension switch
    //   we need to ensure we don't write over the current dimension and that when we switch dimensions we correctly save
    //   ideally we'd also queue up the newchunks in our actual dimension and save them when we switch back

    @SubscribeEvent
    public void onPacketReceivedEvent(final PacketReceivedEvent event) {
        if (!worldCacheInitialized) return;
        try {
            if (event.packet instanceof SPacketChunkData) {
                final SPacketChunkData chunkData = (SPacketChunkData) event.packet;
                if (!chunkData.isFullChunk()) {
                    getCacheForCurrentDimension().ifPresent(c -> c.addNewChunk(chunkData.getChunkX(), chunkData.getChunkZ()));
                }
            }
        } catch (final Exception e) {
            // removing this log as it could possibly spam. we *shouldn't* reach this anyway
//            XaeroPlus.LOGGER.error("Error handling packet event in NewChunks", e);
        }
    }

    @SubscribeEvent
    public void onXaeroWorldChangeEvent(final XaeroWorldChangeEvent event) {
        try {
            saveAllChunks();
            reset();
            initializeWorld();
            loadChunksInCurrentDimension();
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error handling Xaero world change in NewChunks", e);
        }
    }

    private void saveAllChunks() {
        getAllCaches().forEach(cache -> {
            try {
                cache.writeAllChunksToDatabase();
            } catch (final Exception e) {
                XaeroPlus.LOGGER.error("Error saving NewChunks ", e);
            }
        });
    }

    int tickCounter = 0;
    @SubscribeEvent
    public void onClientTickEvent(final TickEvent.ClientTickEvent event) {
        if (!worldCacheInitialized) return;
        // probably don't need to run this on every tick
        // might be off by 1 tick but that's fine
        if (tickCounter > 2000) tickCounter = 0;
        if (tickCounter++ % 10 != 0) {
            return;
        }
        if (tickCounter % 1000 == 0) {
            getAllCaches().forEach(NewChunksLocalCache::writeAllChunksToDatabase);
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
            getCachesExceptDimension(getCurrentlyViewedDimension()).forEach(cache -> cache.setWindow(0, 0, 0));
            getCacheForCurrentDimension().ifPresent(c -> c.setWindow(ChunkUtils.currentPlayerRegionX(), ChunkUtils.currentPlayerRegionZ(), defaultRegionWindowSize));
        }
    }

    public int getMCDimension() {
        try {
            return Minecraft.getMinecraft().world.provider.getDimension();
        } catch (final Exception e) {
            return 0;
        }
    }

    public Optional<NewChunksLocalCache> getCacheForCurrentDimension() {
        switch (getMCDimension()) {
            case -1:
                return netherCache;
            case 0:
                return overworldCache;
            case 1:
                return endCache;
            default:
                throw new RuntimeException("Unknown dimension: " + getMCDimension());
        }
    }

    public Optional<NewChunksLocalCache> getCacheForDimension(final int dimension) {
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

    private List<NewChunksLocalCache> getAllCaches() {
        return Stream.of(netherCache, overworldCache, endCache).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
    }

    public List<NewChunksLocalCache> getCachesExceptDimension(final int dimension) {
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

    @Override
    public void onEnable() {
        initializeWorld();
    }

    private void initializeWorld() {
        try {
            final String worldId = XaeroWorldMapCore.currentSession.getMapProcessor().getCurrentWorldId();
            if (worldId == null) return;
            this.currentWorldId = worldId;
            final NewChunksDatabase db = new NewChunksDatabase(worldId);
            this.newChunksDatabase = Optional.of(db);
            this.netherCache = Optional.of(new NewChunksLocalCache(-1, db));
            this.overworldCache = Optional.of(new NewChunksLocalCache(0, db));
            this.endCache = Optional.of(new NewChunksLocalCache(1, db));
            this.worldCacheInitialized = true;
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

    @Override
    public void onDisable() {
        try {
            saveAllChunks();
            reset();
        } catch (final Exception e) {
            // expected on game launch
        }
    }

    public int getNewChunksColor() {
        return newChunksColor;
    }

    public void reset() {
        this.worldCacheInitialized = false;
        this.currentWorldId = null;
        this.netherCache = Optional.empty();
        this.overworldCache = Optional.empty();
        this.endCache = Optional.empty();
        this.newChunksDatabase.ifPresent(NewChunksDatabase::close);
        this.newChunksDatabase = Optional.empty();
    }

    public void setRgbColor(final int color) {
        newChunksColor = ColorHelper.getColorWithAlpha(color, (int) XaeroPlusSettingRegistry.newChunksAlphaSetting.getValue());
    }

    public void setAlpha(final float a) {
        newChunksColor = ColorHelper.getColorWithAlpha(newChunksColor, (int) (a));
    }

    public List<HighlightAtChunkPos> getNewChunksInRegion(
            final int leafRegionX, final int leafRegionZ,
            final int level,
            final int dimension) {
        return getCacheForDimension(dimension)
                .map(c -> c.getNewChunksInRegion(leafRegionX, leafRegionZ, level))
                .orElse(Collections.emptyList());
    }

    public boolean isNewChunk(final int chunkPosX, final int chunkPosZ, final int dimensionId) {
        return getCacheForDimension(dimensionId)
                .map(c -> c.isNewChunk(chunkPosX, chunkPosZ))
                .orElse(false);
    }
}
