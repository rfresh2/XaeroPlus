package xaeroplus.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.world.World;
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
import xaero.map.cache.BlockStateColorTypeCache;
import xaero.map.file.MapSaveLoad;
import xaero.map.file.RegionDetection;
import xaero.map.file.worldsave.WorldDataHandler;
import xaero.map.gui.GuiMap;
import xaero.map.mods.SupportMods;
import xaero.map.region.LeveledRegion;
import xaero.map.region.LeveledRegionManager;
import xaero.map.region.MapRegion;
import xaero.map.world.MapDimension;
import xaero.map.world.MapWorld;
import xaeroplus.XaeroPlus;
import xaeroplus.event.XaeroWorldChangeEvent;
import xaeroplus.util.CustomDimensionMapProcessor;
import xaeroplus.util.DataFolderResolveUtil;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Hashtable;

import static xaeroplus.XaeroPlus.LOCK_ID;

@Mixin(value = MapProcessor.class, remap = false)
public abstract class MixinMapProcessor implements CustomDimensionMapProcessor {

    @Shadow
    private int state;

    @Final
    @Shadow
    public Object processorThreadPauseSync;

    @Shadow
    private WorldClient world;
    @Shadow
    public double mainPlayerX;
    @Shadow
    public double mainPlayerZ;
    @Shadow
    private boolean mapWorldUsable;
    @Shadow
    private MapLimiter mapLimiter;
    @Shadow
    private MapWorld mapWorld;
    @Shadow
    private ArrayList<LeveledRegion<?>>[] toProcessLevels;
    @Shadow
    private MapSaveLoad mapSaveLoad;
    @Shadow
    private BlockStateColorTypeCache blockStateColorTypeCache;
    @Final
    @Shadow
    public Object uiSync;
    @Shadow
    private boolean mapWorldUsableRequest;
    @Shadow
    private FileLock currentMapLock;
    @Shadow
    private FileLock mapLockToRelease;
    @Shadow
    private FileChannel mapLockChannelToClose;
    @Shadow
    private FileChannel currentMapLockChannel;
    @Shadow
    private boolean currentMapNeedsDeletion;
    @Shadow
    private boolean caveStartDetermined;
    @Shadow
    private int caveStart;
    @Shadow
    private String currentWorldId;
    @Shadow
    private String currentDimId;
    @Shadow
    private String currentMWId;
    @Shadow
    private ArrayList<Double[]> footprints;
    @Shadow
    private MapWriter mapWriter;
    @Shadow
    private WorldClient newWorld;
    @Shadow
    private WorldDataHandler worldDataHandler;
    @Shadow
    private boolean waitingForWorldUpdate;

    @Shadow
    protected abstract void handleRefresh(World world);
    @Shadow
    protected abstract void releaseLocksIfNeeded();
    @Shadow
    protected abstract void forceClean();
    @Shadow
    public abstract void updateCaveStart(double playerX, double playerZ, World world);
    @Shadow
    public abstract void updateFootprints(World world, int step);
    @Shadow
    public abstract boolean isProcessingPaused();
    @Shadow
    protected abstract void updateWorld();
    @Shadow
    public abstract void popWriterPause();
    @Shadow
    public abstract void popRenderPause(boolean b, boolean b1);
    @Shadow
    public abstract void addToProcess(LeveledRegion<?> region);
    @Shadow
    public abstract boolean isCurrentMapLocked();
    @Shadow
    protected abstract void clearToRefresh();
    @Shadow
    public abstract void pushWriterPause();
    @Shadow
    public abstract void pushRenderPause(boolean b, boolean b1);
    @Shadow
    public abstract int getGlobalVersion();
    @Shadow
    public abstract String getDimensionName(int id);

    @Inject(method = "getMainId", at = @At("HEAD"), cancellable = true)
    private void getMainId(boolean rootFolderFormat, CallbackInfoReturnable<String> cir) {
        DataFolderResolveUtil.resolveDataFolder(cir);
    }

    @Inject(method = "getDimensionName", at = @At(value = "HEAD"), cancellable = true)
    public void getDimensionName(final int id, final CallbackInfoReturnable<String> cir) {
        if (!XaeroPlus.nullOverworldDimensionFolder) {
            cir.setReturnValue("DIM" + id);
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
                while (this.state < 2 && WorldMap.crashHandler.getCrashedBy() == null) {
                    synchronized (this.processorThreadPauseSync) {
                        if (!this.isProcessingPaused()) {
                            this.updateWorld();
                            if (this.world != null) {
                                this.updateCaveStart(this.mainPlayerX, this.mainPlayerZ, this.world);
                                this.updateFootprints(this.world, Minecraft.getMinecraft().currentScreen instanceof GuiMap ? 1 : 10);
                            }
                            if (this.mapWorldUsable) {
                                this.mapLimiter.applyLimit(this.mapWorld, (MapProcessor) (Object)this);
                                long currentTime = System.currentTimeMillis();
                                block11:
                                for (ArrayList<LeveledRegion<?>> regionsToProcess : this.toProcessLevels) {
                                    for (int i = 0; i < regionsToProcess.size(); ++i) {
                                        LeveledRegion<?> leveledRegion;
                                        ArrayList<LeveledRegion<?>> arrayList = regionsToProcess;
                                        synchronized (arrayList) {
                                            if (i >= regionsToProcess.size()) {
                                                continue block11;
                                            }
                                            leveledRegion = regionsToProcess.get(i);
                                        }
                                        this.mapSaveLoad.updateSave(leveledRegion, currentTime);
                                    }
                                }
                            }
                            this.mapSaveLoad.run(this.world, this.blockStateColorTypeCache);
                            this.handleRefresh(this.world);
                            runner.doTasks((MapProcessor) (Object) this);
                            this.releaseLocksIfNeeded();
                        }
                    }
                    try {
                        // reduce artificial 1 second thread pause between region loads on login
                        Thread.sleep(10L);
                    }
                    catch (InterruptedException interruptedException) {}
                }
            }
            catch (Throwable e) {
                WorldMap.crashHandler.setCrashedBy(e);
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
     *
     */
    @Inject(method = "updateWorldSynced", at = @At("HEAD"), cancellable = true)
    synchronized void updateWorldSynced(final CallbackInfo ci) throws IOException {
        // @Overwrite kinda weird with these synchronized methods
        // this gets the same effect with inject
        ci.cancel();
        synchronized(this.uiSync) {
            if (this.mapWorldUsable != this.mapWorldUsableRequest
                    || this.mapWorldUsableRequest
                    && (
                    this.mapWorld.getFutureDimension() != this.mapWorld.getCurrentDimension()
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
                        for(LeveledRegion<?> region : currentDim.getMapRegions().getUnsyncedList()) {
                            if (shouldFinishCurrentDim) {
                                if (region.getLevel() == 0 && region.recacheHasBeenRequested() && region.getCacheFile() != null) {
                                    ((MapRegion)region).convertCacheToOutdated(this.mapSaveLoad, "might be outdated");
                                    if (WorldMap.settings.debug) {
                                        WorldMap.LOGGER.info(String.format("Converting cache for region %s because it might be outdated.", region));
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
                        for(LeveledRegion<?> region : reqDim.getMapRegions().getUnsyncedList()) {
                            region.onDimensionClear((MapProcessor) (Object) this);
                        }
                    }

                    if (shouldClearAllDimensions) {
                        for(MapDimension dim : this.mapWorld.getDimensionsList()) {
                            if (!currentDimChecked || dim != currentDim) {
                                for(LeveledRegion<?> region : dim.getMapRegions().getUnsyncedList()) {
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

                this.caveStartDetermined = false;
                this.caveStart = -1;
                this.currentWorldId = newWorldId;
                this.currentDimId = !this.mapWorldUsableRequest ? null : ((MapProcessor) (Object) this).getDimensionName(this.mapWorld.getFutureDimensionId());
                this.currentMWId = newMWId;
                Path mapPath = this.mapSaveLoad.getMWSubFolder(this.currentWorldId, this.currentDimId, this.currentMWId);
                if (this.mapWorldUsable) {
                    Files.createDirectories(mapPath);
                    /** allow us to have multiple clients open on the same map ;) **/
                    Path mapLockPath = Paths.get(System.getProperty("java.io.tmpdir")).resolve(LOCK_ID + ".lock");
                    int totalLockAttempts = 10;
                    int lockAttempts = 10;

                    while(lockAttempts-- > 0) {
                        if (lockAttempts < 9) {
                            WorldMap.LOGGER.info("Failed attempt to lock the current world map! Retrying in 50 ms... " + lockAttempts);

                            try {
                                Thread.sleep(50L);
                            } catch (InterruptedException var16) {
                            }
                        }

                        try {
                            FileChannel lockChannel = FileChannel.open(mapLockPath, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
                            this.currentMapLock = lockChannel.tryLock();
                            if (this.currentMapLock != null) {
                                this.currentMapLockChannel = lockChannel;
                                break;
                            }
                        } catch (Exception var17) {
                            WorldMap.LOGGER.error("suppressed exception", var17);
                        }
                    }
                }

                this.footprints.clear();
                this.mapSaveLoad.clearToLoad();
                this.mapSaveLoad.setNextToLoadByViewing((LeveledRegion<?>)null);
                this.clearToRefresh();

                for(int i = 0; i < this.toProcessLevels.length; ++i) {
                    this.toProcessLevels[i].clear();
                }

                if (this.mapWorldUsable && !this.isCurrentMapLocked()) {
                    for(LeveledRegion<?> region : this.mapWorld.getCurrentDimension().getMapRegions().getUnsyncedList()) {
                        if (region.shouldBeProcessed()) {
                            this.addToProcess(region);
                        }
                    }
                }

                this.mapWriter.resetPosition();
                this.world = this.newWorld;
                if (SupportMods.framedBlocks()) {
                    SupportMods.supportFramedBlocks.onWorldChange();
                }

                if (WorldMap.settings.debug) {
                    WorldMap.LOGGER.info("World/dimension changed to: " + this.currentWorldId + " " + this.currentDimId + " " + this.currentMWId);
                }
                XaeroPlus.EVENT_BUS.post(new XaeroWorldChangeEvent(this.currentWorldId, this.currentDimId, this.currentMWId));

                this.worldDataHandler.prepareSingleplayer(this.world, (MapProcessor) (Object) this);
                if (this.worldDataHandler.getWorldDir() == null && this.currentWorldId != null && !this.mapWorld.isMultiplayer()) {
                    this.currentWorldId = this.currentDimId = null;
                }

                boolean shouldDetect = this.mapWorldUsable && this.mapWorld.getCurrentDimension().getDetectedRegions() == null;
                this.mapSaveLoad.setRegionDetectionComplete(!shouldDetect);
                this.popRenderPause(true, true);
                this.popWriterPause();
            } else if (this.newWorld != this.world) {
                this.pushWriterPause();
                this.world = this.newWorld;
                if (SupportMods.framedBlocks()) {
                    SupportMods.supportFramedBlocks.onWorldChange();
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
    public synchronized void changeWorld(final WorldClient world, final CallbackInfo ci) {
        XaeroPlus.customDimensionId = world.provider.getDimension();
    }

    @Redirect(method = "onRenderProcess", at = @At(value = "INVOKE", target = "Lxaero/map/world/MapWorld;getCurrentDimension()Lxaero/map/world/MapDimension;"))
    public MapDimension getCustomDimension(final MapWorld mapWorld) {
        return mapWorld.getDimension(XaeroPlus.customDimensionId);
    }

    @Override
    public boolean regionExistsCustomDimension(int x, int z, int dimId) {
        return this.regionDetectionExistsCustomDimension(x, z, dimId) || this.mapWorld.getDimension(dimId).getHighlightHandler().shouldApplyRegionHighlights(x, z, false);
    }

    @Override
    public boolean regionDetectionExistsCustomDimension(int x, int z, int dimId) {
        if (!this.mapSaveLoad.isRegionDetectionComplete()) {
            return false;
        } else {
            MapDimension dimension = this.mapWorld.getDimension(dimId);
            if (dimension != null) {
                Hashtable<Integer, Hashtable<Integer, RegionDetection>> detectedRegions = dimension.getDetectedRegions();
                if (detectedRegions != null) {
                    Hashtable<Integer, RegionDetection> column = detectedRegions.get(x);
                    if (column != null) {
                        return column != null && column.containsKey(z);
                    }
                }
            }
            return false;
        }
    }

    @Override
    public RegionDetection getRegionDetectionCustomDimension(int x, int z, int dimId) {
        Hashtable<Integer, RegionDetection> column = this.mapWorld.getDimension(dimId).getDetectedRegions().get(x);
        return column != null ? column.get(z) : null;
    }

    @Override
    public void removeRegionDetectionCustomDimension(int x, int z, int dimId) {
        Hashtable<Integer, Hashtable<Integer, RegionDetection>> current = this.mapWorld.getDimension(dimId).getDetectedRegions();
        Hashtable<Integer, RegionDetection> column = current.get(x);
        if (column != null) {
            column.remove(z);
        }

        if (column.isEmpty()) {
            current.remove(x);
        }
    }

    @Override
    public LeveledRegion<?> getLeveledRegionCustomDimension(int leveledRegX, int leveledRegZ, int level, int dimId) {
        MapDimension mapDimension = this.mapWorld.getDimension(dimId);
        if (mapDimension == null) {
            mapDimension = this.mapWorld.createDimensionUnsynced(world, dimId);
        }
        LeveledRegionManager regions = mapDimension.getMapRegions();
        return regions.get(leveledRegX, leveledRegZ, level);
    }

    @Override
    public MapRegion getMapRegionCustomDimension(int regX, int regZ, boolean create, int dimId) {
        if (!this.mapSaveLoad.isRegionDetectionComplete()) {
            return null;
        } else {
            MapDimension mapDimension = this.mapWorld.getDimension(dimId);
            LeveledRegionManager regions = mapDimension.getMapRegions();
            MapRegion region = regions.getLeaf(regX, regZ);
            if (region == null) {
                if (!create) {
                    return null;
                }

                if (!Minecraft.getMinecraft().isCallingFromMinecraftThread()) {
                    throw new IllegalAccessError();
                }

                region = new MapRegion(
                        this.currentWorldId, getDimensionName(dimId), this.currentMWId, mapDimension, regX, regZ, this.getGlobalVersion(), this.mapWorld.isMultiplayer()
                );
                RegionDetection regionDetection = this.getRegionDetectionCustomDimension(regX, regZ, dimId);
                if (regionDetection != null) {
                    regionDetection.transferInfoTo(region);
                    this.removeRegionDetectionCustomDimension(regX, regZ, dimId);
                }

                if (!region.hasHadTerrain()) {
                    mapDimension.getHighlightHandler().getRegionHighlightExistenceTracker().stopTracking(regX, regZ);
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
}
