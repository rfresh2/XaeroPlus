package xaeroplus.mixin.client;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.Fluid;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.*;
import xaero.map.biome.BiomeColorCalculator;
import xaero.map.biome.BiomeGetter;
import xaero.map.biome.BlockTintProvider;
import xaero.map.cache.BrokenBlockTintCache;
import xaero.map.effects.Effects;
import xaero.map.file.MapSaveLoad;
import xaero.map.file.RegionDetection;
import xaero.map.file.worldsave.WorldDataHandler;
import xaero.map.gui.GuiMap;
import xaero.map.misc.CaveStartCalculator;
import xaero.map.misc.Misc;
import xaero.map.mods.SupportMods;
import xaero.map.region.LayeredRegionManager;
import xaero.map.region.LeveledRegion;
import xaero.map.region.MapLayer;
import xaero.map.region.MapRegion;
import xaero.map.world.MapDimension;
import xaero.map.world.MapWorld;
import xaeroplus.XaeroPlus;
import xaeroplus.util.CustomDimensionMapProcessor;
import xaeroplus.util.CustomDimensionMapSaveLoad;
import xaeroplus.util.DataFolderResolveUtil;
import xaeroplus.util.Shared;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

import static xaeroplus.util.Shared.LOCK_ID;

@Mixin(value = MapProcessor.class, remap = false)
public abstract class MixinMapProcessor implements CustomDimensionMapProcessor {
    @Shadow private int state;
    @Final
    @Shadow public Object processorThreadPauseSync;
    @Shadow private ClientWorld world;
    @Shadow private boolean mapWorldUsable;
    @Shadow private MapLimiter mapLimiter;
    @Shadow private MapWorld mapWorld;
    @Shadow private ArrayList<LeveledRegion<?>>[] toProcessLevels;
    @Shadow private MapSaveLoad mapSaveLoad;
    @Shadow private int currentCaveLayer;
    @Shadow private RegistryWrapper<Block> worldBlockLookup;
    @Shadow private Registry<Block> worldBlockRegistry;
    @Shadow private Registry<Fluid> worldFluidRegistry;
    @Shadow public Registry<Biome> worldBiomeRegistry;
    @Final
    @Shadow private BiomeGetter biomeGetter;
    @Final
    @Shadow public Object uiSync;
    @Shadow private boolean mapWorldUsableRequest;
    @Shadow private FileLock currentMapLock;
    @Shadow private FileLock mapLockToRelease;
    @Shadow private FileChannel mapLockChannelToClose;
    @Shadow private FileChannel currentMapLockChannel;
    @Shadow private boolean currentMapNeedsDeletion;
    @Shadow private String currentWorldId;
    @Shadow private String currentDimId;
    @Shadow private String currentMWId;
    @Shadow private ArrayList<Double[]> footprints;
    @Shadow private MapWriter mapWriter;
    @Shadow private ClientWorld newWorld;
    @Shadow private RegistryWrapper<Block> newWorldBlockLookup;
    @Shadow public Registry<Block> newWorldBlockRegistry;
    @Shadow private Registry<Fluid> newWorldFluidRegistry;
    @Shadow public Registry<Biome> newWorldBiomeRegistry;
    @Shadow private BlockTintProvider worldBlockTintProvider;
    @Shadow private BiomeColorCalculator biomeColorCalculator;
    @Final
    @Shadow private BrokenBlockTintCache brokenBlockTintCache;
    @Shadow private WorldDataHandler worldDataHandler;
    @Shadow private boolean waitingForWorldUpdate;
    @Final
    @Shadow private CaveStartCalculator caveStartCalculator;
    @Shadow private int localCaveMode;
    @Shadow private long lastLocalCaveModeToggle;
    @Shadow
    protected abstract int getCaveLayer(int caveStart);
    @Shadow
    public abstract int getGlobalVersion();
    @Shadow
    public abstract void popWriterPause();
    @Shadow
    public abstract void popRenderPause(boolean rendering, boolean uploading);
    @Shadow
    public abstract void addToProcess(LeveledRegion<?> region);
    @Shadow
    public abstract boolean isCurrentMapLocked();
    @Shadow
    protected abstract void clearToRefresh();
    @Shadow
    public abstract String getDimensionName(RegistryKey<World> id);
    @Shadow public abstract void pushWriterPause();
    @Shadow
    public abstract void pushRenderPause(boolean rendering, boolean uploading);
    @Shadow
    protected abstract void forceClean();
    @Shadow
    protected abstract void releaseLocksIfNeeded();
    @Shadow
    protected abstract void handleRefresh(World world) throws RuntimeException;
    @Shadow
    public abstract void updateFootprints(World world, int step);
    @Shadow
    public abstract boolean isProcessingPaused();
    @Shadow
    protected abstract void updateWorld() throws IOException, CommandSyntaxException;

    /**
     * @author rfresh2
     * @reason custom dimension support
     */
    @Overwrite
    public void updateCaveStart() {
        MinecraftClient mc = MinecraftClient.getInstance();
        MapDimension dimension = this.mapWorld.getDimension(Shared.customDimensionId);
        int newCaveStart;
        // cave mode type = 2 means "Full"
        if (WorldMap.settings.isCaveMapsAllowed() && dimension.getCaveModeType() != 0) {
            if (WorldMap.settings.caveModeStart == Integer.MAX_VALUE) {
                newCaveStart = Integer.MIN_VALUE; // this renders us like the "off" mode for some reason
            } else {
                newCaveStart = WorldMap.settings.caveModeStart;
            }

            boolean isMapScreen = mc.currentScreen instanceof GuiMap || Misc.screenShouldSkipWorldRender(mc.currentScreen, true);
            if (!isMapScreen
                    || !MinecraftClient.getInstance().player.hasStatusEffect(Effects.NO_CAVE_MAPS)
                    && !MinecraftClient.getInstance().player.hasStatusEffect(Effects.NO_CAVE_MAPS_HARMFUL)) {
                if (SupportMods.minimap() && (WorldMap.settings.autoCaveMode < 0 && newCaveStart == Integer.MIN_VALUE || !isMapScreen)) {
                    newCaveStart = SupportMods.xaeroMinimap.getCaveStart(newCaveStart, isMapScreen);
                }

                if (newCaveStart == Integer.MIN_VALUE) {
                    long currentTime = System.currentTimeMillis();
                    int nextLocalCaveMode = this.caveStartCalculator.getCaving(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.world);
                    boolean toggling = this.localCaveMode == Integer.MAX_VALUE != (nextLocalCaveMode == Integer.MAX_VALUE);
                    if (!toggling || currentTime - this.lastLocalCaveModeToggle > (long)WorldMap.settings.caveModeToggleTimer) {
                        if (toggling) {
                            this.lastLocalCaveModeToggle = currentTime;
                        }

                        this.localCaveMode = nextLocalCaveMode;
                    }

                    newCaveStart = this.localCaveMode;
                }

                if (dimension.getCaveModeType() == 2) { // fixed this when height is set to "auto" so it actually does the full caving
                    newCaveStart = Integer.MIN_VALUE; // "Full"
                }
                if (newCaveStart != Integer.MAX_VALUE) {
                    if (dimension.getCaveModeType() != 2) {
                        newCaveStart = MathHelper.clamp(newCaveStart, 0, this.world.getHeight() - 1);
                    }
                }
            } else {
                newCaveStart = Integer.MAX_VALUE; // "off"
            }
        } else {
            newCaveStart = Integer.MAX_VALUE; // "off"
        }

        int newCaveLayer = this.getCaveLayer(newCaveStart);
        dimension.getLayeredMapRegions().getLayer(newCaveLayer).setCaveStart(newCaveStart);
        this.currentCaveLayer = newCaveLayer;
    }

    @Redirect(method = "updateWorld", at = @At(value = "INVOKE", target = "Lxaero/map/file/MapSaveLoad;detectRegions(I)V"))
    public void updateWorldDetectRegionsRedirect(MapSaveLoad mapSaveLoad, int step) {
        ((CustomDimensionMapSaveLoad) mapSaveLoad).detectRegionsInDimension(step, Shared.customDimensionId);
    }

    @Inject(method = "getMainId", at = @At("HEAD"), cancellable = true)
    private void getMainId(final boolean rootFolderFormat, final ClientPlayNetworkHandler connection, final CallbackInfoReturnable<String> cir) {
        DataFolderResolveUtil.resolveDataFolder(connection, cir);
    }

    @Inject(method = "getDimensionName", at = @At(value = "HEAD"), cancellable = true)
    public void getDimensionName(final RegistryKey<World> id, final CallbackInfoReturnable<String> cir) {
        if (!Shared.nullOverworldDimensionFolder) {
            if (id == World.OVERWORLD) {
                cir.setReturnValue("DIM0");
            }
        }
    }

    /**
     * @author rfresh2
     * @reason Reduce thread wait time to increase region load performance
     */
    @Overwrite
    public void run(MapRunner runner) {
        if (this.state < 2) {
            try {
                while(this.state < 2 && WorldMap.crashHandler.getCrashedBy() == null) {
                    synchronized(this.processorThreadPauseSync) {
                        if (!this.isProcessingPaused()) {
                            this.updateWorld();
                            if (this.world != null) {
                                this.updateFootprints(this.world, MinecraftClient.getInstance().currentScreen instanceof GuiMap ? 1 : 10);
                            }

                            if (this.mapWorldUsable) {
                                this.mapLimiter.applyLimit(this.mapWorld, (MapProcessor) (Object) this);
                                long currentTime = System.currentTimeMillis();

                                for(int l = 0; l < this.toProcessLevels.length; ++l) {
                                    ArrayList<LeveledRegion<?>> regionsToProcess = this.toProcessLevels[l];

                                    for(int i = 0; i < regionsToProcess.size(); ++i) {
                                        LeveledRegion<?> leveledRegion;
                                        synchronized(regionsToProcess) {
                                            if (i >= regionsToProcess.size()) {
                                                break;
                                            }

                                            leveledRegion = regionsToProcess.get(i);
                                        }

                                        this.mapSaveLoad.updateSave(leveledRegion, currentTime, this.currentCaveLayer);
                                    }
                                }
                            }

                            this.mapSaveLoad
                                    .run(this.world, this.worldBlockLookup, this.worldBlockRegistry, this.worldFluidRegistry, this.biomeGetter, this.worldBiomeRegistry);
                            this.handleRefresh(this.world);
                            runner.doTasks((MapProcessor) (Object) this);
                            this.releaseLocksIfNeeded();
                        }
                    }

                    try {
                        Thread.sleep(5L);
                    } catch (InterruptedException var12) {
                    }
                }
            } catch (Throwable e) {
                if (e instanceof RuntimeException && e.getMessage().startsWith("Trying to save cache for a region with cache not prepared:")) {
                    XaeroPlus.LOGGER.error("Caught exception while processing map. Preventing crash.", e);
                } else {
                    WorldMap.crashHandler.setCrashedBy(e);
                }
            }

            if (this.state < 2) {
                this.forceClean();
            }
        }

        if (this.state == 2) {
            this.state = 3;
        }
    }

    /**
     * Reason: Allow multiple client instances to open the same map
     */
    @Inject(method = "updateWorldSynced", at = @At("HEAD"), cancellable = true)
    synchronized void updateWorldSynced(final CallbackInfo ci) throws IOException {
        // @Overwrite kinda weird with these synchronized methods
        // this gets the same effect with inject
        ci.cancel();
        synchronized(this.uiSync) {
            boolean changedDimension = this.mapWorldUsable != this.mapWorldUsableRequest
                    || !this.mapWorldUsableRequest
                    || this.mapWorld.getFutureDimension() != this.mapWorld.getCurrentDimension();
            if (this.mapWorldUsable != this.mapWorldUsableRequest
                    || this.mapWorldUsableRequest
                    && (
                    changedDimension
                            || !this.mapWorld.getFutureDimension().getFutureMultiworldUnsynced().equals(this.mapWorld.getFutureDimension().getCurrentMultiworld())
            )) {
                String newMultiworldId = !this.mapWorldUsableRequest ? null : this.mapWorld.getFutureMultiworldUnsynced();
                this.pushRenderPause(true, true);
                this.pushWriterPause();
                String newWorldId = !this.mapWorldUsableRequest ? null : this.mapWorld.getMainId();
                String newMWId = !this.mapWorld.isMultiplayer() ? null : newMultiworldId;
                boolean shouldClearAllDimensions = this.state == 1;
                boolean shouldClearNewDimension = this.mapWorldUsableRequest
                        && !this.mapWorld.getFutureMultiworldUnsynced().equals(this.mapWorld.getFutureDimension().getCurrentMultiworld());
                this.mapSaveLoad.getToSave().clear();
                if (this.currentMapLock != null) {
                    this.mapLockToRelease = this.currentMapLock;
                    this.mapLockChannelToClose = this.currentMapLockChannel;
                    this.currentMapLock = null;
                    this.currentMapLockChannel = null;
                }

                this.releaseLocksIfNeeded();
                if (this.mapWorld.getCurrentDimensionId() != null) {
                    MapDimension currentDim = this.mapWorld.getCurrentDimension();
                    MapDimension reqDim = !this.mapWorldUsableRequest ? null : this.mapWorld.getFutureDimension();
                    boolean shouldFinishCurrentDim = this.mapWorldUsable && !this.currentMapNeedsDeletion;
                    boolean currentDimChecked = false;
                    if (shouldFinishCurrentDim) {
                        this.mapSaveLoad.saveAll = true;
                    }

                    if (shouldFinishCurrentDim || shouldClearNewDimension && reqDim == currentDim) {
                        for(LeveledRegion<?> region : currentDim.getLayeredMapRegions().getUnsyncedList()) {
                            if (shouldFinishCurrentDim) {
                                if (region.getLevel() == 0) {
                                    MapRegion leafRegion = (MapRegion)region;
                                    if (!leafRegion.isMultiplayer() && !leafRegion.hasLookedForCache() && leafRegion.isOutdatedWithOtherLayers()) {
                                        File potentialCacheFile = this.mapSaveLoad.getCacheFile(leafRegion, leafRegion.getCaveLayer(), false, false);
                                        if (potentialCacheFile.exists()) {
                                            leafRegion.setCacheFile(potentialCacheFile);
                                            leafRegion.setLookedForCache(true);
                                        }
                                    }

                                    if (leafRegion.shouldConvertCacheToOutdatedOnFinishDim() && leafRegion.getCacheFile() != null) {
                                        leafRegion.convertCacheToOutdated(this.mapSaveLoad, "might be outdated");
                                        if (WorldMap.settings.debug) {
                                            WorldMap.LOGGER.info(String.format("Converting cache for region %s because it might be outdated.", leafRegion));
                                        }
                                    }
                                }

                                region.setReloadHasBeenRequested(false, "world/dim change");
                                region.onCurrentDimFinish(this.mapSaveLoad, (MapProcessor) (Object) this);
                            }

                            if (shouldClearAllDimensions || shouldClearNewDimension && reqDim == currentDim) {
                                region.onDimensionClear((MapProcessor) (Object) this);
                            }
                        }

                        currentDimChecked = true;
                    }

                    if (reqDim != currentDim && shouldClearNewDimension) {
                        for(LeveledRegion<?> region : reqDim.getLayeredMapRegions().getUnsyncedList()) {
                            region.onDimensionClear((MapProcessor) (Object) this);
                        }
                    }

                    if (shouldClearAllDimensions) {
                        for(MapDimension dim : this.mapWorld.getDimensionsList()) {
                            if (!currentDimChecked || dim != currentDim) {
                                for(LeveledRegion<?> region : dim.getLayeredMapRegions().getUnsyncedList()) {
                                    region.onDimensionClear((MapProcessor) (Object) this);
                                }
                            }
                        }
                    }

                    if (this.currentMapNeedsDeletion) {
                        this.mapWorld.getCurrentDimension().deleteMultiworldMapDataUnsynced(this.mapWorld.getCurrentDimension().getCurrentMultiworld());
                    }
                }

                this.currentMapNeedsDeletion = false;
                if (!shouldClearAllDimensions) {
                    if (shouldClearNewDimension) {
                        this.mapWorld.getFutureDimension().regionsToCache.clear();
                        this.mapWorld.getFutureDimension().clear();
                        if (WorldMap.settings.debug) {
                            WorldMap.LOGGER.info("Dimension map data cleared!");
                        }
                    }
                } else {
                    if (this.mapWorld.getCurrentDimensionId() != null) {
                        for(MapDimension dim : this.mapWorld.getDimensionsList()) {
                            dim.clear();
                        }
                    }

                    if (WorldMap.settings.debug) {
                        WorldMap.LOGGER.info("All map data cleared!");
                    }

                    if (this.state == 1) {
                        WorldMap.LOGGER.info("World map cleaned normally!");
                        this.state = 2;
                    }
                }

                if (WorldMap.settings.debug) {
                    WorldMap.LOGGER.info("World changed!");
                }

                this.mapWorldUsable = this.mapWorldUsableRequest;
                if (this.mapWorldUsableRequest) {
                    this.mapWorld.switchToFutureUnsynced();
                }

                this.currentWorldId = newWorldId;
                this.currentDimId = !this.mapWorldUsableRequest ? null : ((MapProcessor) (Object) this).getDimensionName(this.mapWorld.getFutureDimensionId());
                this.currentMWId = newMWId;
                Path mapPath = this.mapSaveLoad.getMWSubFolder(this.currentWorldId, this.currentDimId, this.currentMWId);
                if (this.mapWorldUsable) {
                    Files.createDirectories(mapPath);
                    /** allow us to have multiple clients open on the same map ;) **/
                    Path mapLockPath = Paths.get(System.getProperty("java.io.tmpdir")).resolve(LOCK_ID + ".lock");
                    int lockAttempts = 10;

                    while (lockAttempts-- > 0) {
                        if (lockAttempts < 9) {
                            WorldMap.LOGGER.info("Failed attempt to lock the current world map! Retrying in 50 ms... " + lockAttempts);

                            try {
                                Thread.sleep(50L);
                            } catch (InterruptedException var17) {
                            }
                        }

                        try {
                            FileChannel lockChannel = FileChannel.open(mapLockPath, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
                            this.currentMapLock = lockChannel.tryLock();
                            if (this.currentMapLock != null) {
                                this.currentMapLockChannel = lockChannel;
                                break;
                            }
                        } catch (Exception var18) {
                            WorldMap.LOGGER.error("suppressed exception", var18);
                        }
                    }
                }

                this.footprints.clear();
                this.mapSaveLoad.clearToLoad();
                this.mapSaveLoad.setNextToLoadByViewing((LeveledRegion<?>) null);
                this.clearToRefresh();

                for(int i = 0; i < this.toProcessLevels.length; ++i) {
                    this.toProcessLevels[i].clear();
                }

                if (this.mapWorldUsable && !this.isCurrentMapLocked()) {
                    for(LeveledRegion<?> region : this.mapWorld.getCurrentDimension().getLayeredMapRegions().getUnsyncedList()) {
                        if (region.shouldBeProcessed()) {
                            this.addToProcess(region);
                        }
                    }
                }

                this.mapWriter.resetPosition();
                this.world = this.newWorld;
                this.worldBlockLookup = this.newWorldBlockLookup;
                this.worldBlockRegistry = this.newWorldBlockRegistry;
                this.worldFluidRegistry = this.newWorldFluidRegistry;
                this.worldBiomeRegistry = this.newWorldBiomeRegistry;
                this.worldBlockTintProvider = this.world == null
                        ? null
                        : new BlockTintProvider(this.worldBiomeRegistry, this.biomeColorCalculator, (MapProcessor) (Object) this, this.brokenBlockTintCache);
                if (SupportMods.framedBlocks()) {
                    SupportMods.supportFramedBlocks.onWorldChange();
                }

                if (SupportMods.pac()) {
                    SupportMods.xaeroPac.onMapChange(changedDimension);
                    SupportMods.xaeroPac.resetDetection();
                }

                if (WorldMap.settings.debug) {
                    WorldMap.LOGGER.info("World/dimension changed to: " + this.currentWorldId + " " + this.currentDimId + " " + this.currentMWId);
                }

                this.worldDataHandler.prepareSingleplayer(this.world, (MapProcessor) (Object) this);
                if (this.worldDataHandler.getWorldDir() == null && this.currentWorldId != null && !this.mapWorld.isMultiplayer()) {
                    this.currentWorldId = this.currentDimId = null;
                }

                boolean shouldDetect = this.mapWorldUsable && !this.mapWorld.getCurrentDimension().hasDoneRegionDetection();
                this.mapSaveLoad.setRegionDetectionComplete(!shouldDetect);
                this.popRenderPause(true, true);
                this.popWriterPause();
            } else if (this.newWorld != this.world) {
                this.pushWriterPause();
                this.world = this.newWorld;
                this.worldBlockLookup = this.newWorldBlockLookup;
                this.worldBlockRegistry = this.newWorldBlockRegistry;
                this.worldFluidRegistry = this.newWorldFluidRegistry;
                this.worldBiomeRegistry = this.newWorldBiomeRegistry;
                this.worldBlockTintProvider = this.world == null
                        ? null
                        : new BlockTintProvider(this.worldBiomeRegistry, this.biomeColorCalculator, (MapProcessor) (Object) this, this.brokenBlockTintCache);
                if (SupportMods.framedBlocks()) {
                    SupportMods.supportFramedBlocks.onWorldChange();
                }

                if (SupportMods.pac()) {
                    SupportMods.xaeroPac.resetDetection();
                }

                this.popWriterPause();
            }

            if (this.mapWorldUsable) {
                this.mapWorld.getCurrentDimension().switchToFutureMultiworldWritableValueUnsynced();
                this.mapWorld.switchToFutureMultiworldTypeUnsynced();
            }

            this.waitingForWorldUpdate = false;
        }
    }

    @Inject(method = "changeWorld", at = @At(value = "INVOKE", target = "Lxaero/map/world/MapDimension;resetCustomMultiworldUnsynced()V", shift = At.Shift.AFTER))
    public synchronized void changeWorld(final ClientWorld world, final RegistryWrapper<Block> blockLookup, final Registry<Block> blockRegistry, final Registry<Fluid> fluidRegistry, final Registry<Biome> biomeRegistry, final CallbackInfo ci) {
        Shared.customDimensionId = world.getRegistryKey();
    }

    @Redirect(method = "onRenderProcess", at = @At(value = "INVOKE", target = "Lxaero/map/world/MapWorld;getCurrentDimension()Lxaero/map/world/MapDimension;"))
    public MapDimension getCustomDimension(final MapWorld mapWorld) {
        return mapWorld.getDimension(Shared.customDimensionId);
    }

    @Override
    public boolean regionExistsCustomDimension(int caveLayer, int x, int z, RegistryKey<World> dimId) {
        return this.regionDetectionExistsCustomDimension(caveLayer, x, z, dimId)
                || this.mapWorld.getDimension(dimId).getHighlightHandler().shouldApplyRegionHighlights(x, z, false);
    }

    @Override
    public boolean regionExistsCustomDimension(int x, int z, RegistryKey<World> dimId) {
        return regionExistsCustomDimension(Integer.MAX_VALUE, x, z, dimId);
    }

    @Override
    public boolean regionDetectionExistsCustomDimension(int caveLayer, int x, int z, RegistryKey<World> dimId) {
        return this.mapSaveLoad.isRegionDetectionComplete()
                && this.mapWorld.getDimension(dimId).getLayeredMapRegions().getLayer(caveLayer).regionDetectionExists(x, z);
    }

    @Override
    public LeveledRegion<?> getLeveledRegionCustomDimension(int caveLayer, int leveledRegX, int leveledRegZ, int level, RegistryKey<World> dimId) {
        MapDimension mapDimension = this.mapWorld.getDimension(dimId);
        LayeredRegionManager regions = mapDimension.getLayeredMapRegions();
        return regions.get(caveLayer, leveledRegX, leveledRegZ, level);
    }

    @Override
    public MapRegion getMapRegionCustomDimension(int caveLayer, int regX, int regZ, boolean create, RegistryKey<World> dimId) {
        if (!this.mapSaveLoad.isRegionDetectionComplete()) {
            return null;
        } else {
            MapDimension mapDimension = this.mapWorld.getDimension(dimId);
            LayeredRegionManager regions = mapDimension.getLayeredMapRegions();
            MapRegion region = regions.getLeaf(caveLayer, regX, regZ);
            if (region == null) {
                if (!create) {
                    return null;
                }

                if (!MinecraftClient.getInstance().isOnThread()) {
                    throw new IllegalAccessError();
                }

                region = new MapRegion(
                        this.currentWorldId,
                        getDimensionName(dimId),
                        this.currentMWId,
                        mapDimension,
                        regX,
                        regZ,
                        caveLayer,
                        this.getGlobalVersion(),
                        this.mapWorld.isMultiplayer(),
                        this.worldBiomeRegistry
                );
                MapLayer mapLayer = regions.getLayer(caveLayer);
                region.updateCaveMode();
                RegionDetection regionDetection = mapLayer.getRegionDetection(regX, regZ);
                if (regionDetection != null) {
                    regionDetection.transferInfoTo(region);
                    mapLayer.removeRegionDetection(regX, regZ);
                } else if (!region.isMultiplayer() && mapDimension.getWorldSaveRegionDetection(regX, regZ) == null) {
                    RegionDetection worldSaveRegionDetection = new RegionDetection(
                            region.getWorldId(),
                            region.getDimId(),
                            region.getMwId(),
                            region.getRegionX(),
                            region.getRegionZ(),
                            region.getRegionFile(),
                            this.getGlobalVersion(),
                            true
                    );
                    mapDimension.addWorldSaveRegionDetection(worldSaveRegionDetection);
                    mapLayer.removeRegionDetection(regX, regZ);
                }

                if (!region.hasHadTerrain()) {
                    regions.getLayer(caveLayer).getRegionHighlightExistenceTracker().stopTracking(regX, regZ);
                    region.setVersion(this.getGlobalVersion());
                    region.setCacheHashCode(WorldMap.settings.getRegionCacheHashCode());
                    region.setReloadVersion(WorldMap.settings.reloadVersion);
                }

                regions.putLeaf(regX, regZ, region);
                regions.addListRegion(region);
                if (regionDetection != null) {
                    regionDetection.transferInfoPostAddTo(region, (MapProcessor) (Object) this);
                }
            }

            return region;
        }
    }

    @Override
    public MapRegion getMapRegionCustomDimension(int regX, int regZ, boolean create, RegistryKey<World> dimId) {
        return getMapRegionCustomDimension(Integer.MAX_VALUE, regX, regZ, create, dimId);
    }
}
