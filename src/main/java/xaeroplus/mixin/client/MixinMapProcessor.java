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
import xaero.map.biome.BiomeColorCalculator;
import xaero.map.cache.BlockStateColorTypeCache;
import xaero.map.file.MapSaveLoad;
import xaero.map.file.worldsave.WorldDataHandler;
import xaero.map.gui.GuiMap;
import xaero.map.misc.CaveStartCalculator;
import xaero.map.mods.SupportMods;
import xaero.map.region.LeveledRegion;
import xaero.map.region.MapRegion;
import xaero.map.region.OverlayManager;
import xaero.map.world.MapDimension;
import xaero.map.world.MapWorld;
import xaeroplus.XaeroPlus;
import xaeroplus.event.XaeroWorldChangeEvent;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.util.DataFolderResolveUtil;
import xaeroplus.util.Globals;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

import static xaeroplus.util.Globals.LOCK_ID;

@Mixin(value = MapProcessor.class, remap = false)
public abstract class MixinMapProcessor {

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
    @Final
    @Shadow
    private CaveStartCalculator caveStartCalculator;
    @Shadow
    private boolean currentMapNeedsDeletion;
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
    private int currentCaveLayer;
    @Shadow
    private long lastLocalCaveModeToggle;
    @Shadow
    private int nextLocalCaveMode;
    @Shadow
    private int localCaveMode;

    @Shadow
    protected abstract void updateWorld();

    @Shadow
    protected abstract void handleRefresh();

    @Shadow
    protected abstract void releaseLocksIfNeeded();

    @Shadow
    protected abstract void forceClean();

    @Shadow
    public abstract void updateFootprints(int step);

    @Shadow
    public abstract boolean isProcessingPaused();

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

    @Shadow
    protected abstract int getCaveLayer(int caveStart);
    @Shadow
    protected abstract void checkFootstepsReset(World oldWorld, World newWorld);

    @Shadow private BiomeColorCalculator biomeColorCalculator;

    @Shadow private OverlayManager overlayManager;

    @Inject(method = "getMainId", at = @At("HEAD"), cancellable = true)
    private void getMainId(boolean rootFolderFormat, boolean preIP6Fix, CallbackInfoReturnable<String> cir) {
        DataFolderResolveUtil.resolveDataFolder(cir);
    }

    @Inject(method = "getDimensionName", at = @At(value = "HEAD"), cancellable = true)
    public void getDimensionName(final int id, final CallbackInfoReturnable<String> cir) {
        if (!Globals.nullOverworldDimensionFolder) {
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
                                this.updateFootprints(Minecraft.getMinecraft().currentScreen instanceof GuiMap ? 1 : 10);
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
                            this.mapSaveLoad.run(this.world, this.blockStateColorTypeCache);
                            this.handleRefresh();
                            runner.doTasks((MapProcessor) (Object) this);
                            this.releaseLocksIfNeeded();
                        }
                    }
                    try {
                        // reduce artificial 1 second thread pause between region loads on login
                        Thread.sleep(5L);
                    } catch (InterruptedException interruptedException) {
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
                try {
                    this.forceClean();
                } catch (final Throwable e) {
                    WorldMap.crashHandler.setCrashedBy(e);
                }
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
        synchronized (this.uiSync) {
            if (this.mapWorldUsable != this.mapWorldUsableRequest
                    || this.mapWorldUsableRequest
                    && (
                    this.mapWorld.getFutureDimension() != this.mapWorld.getCurrentDimension()
                            || !this.mapWorld.getFutureDimension().getFutureMultiworldUnsynced().equals(this.mapWorld.getFutureDimension().getCurrentMultiworld())
            )) {
                String newMWId = !this.mapWorldUsableRequest ? null : this.mapWorld.getFutureMultiworldUnsynced();
                this.pushRenderPause(true, true);
                this.pushWriterPause();
                String newWorldId = !this.mapWorldUsableRequest ? null : this.mapWorld.getMainId();
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
                        for(LeveledRegion<?> region : currentDim.getLayeredMapRegions().getUnsyncedSet()) {
                            if (shouldFinishCurrentDim) {
                                if (region.getLevel() == 0) {
                                    MapRegion leafRegion = (MapRegion) region;
                                    if (!leafRegion.isNormalMapData() && !leafRegion.hasLookedForCache() && leafRegion.isOutdatedWithOtherLayers()) {
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
                        for(LeveledRegion<?> region : reqDim.getLayeredMapRegions().getUnsyncedSet()) {
                            region.onDimensionClear((MapProcessor) (Object) this);
                        }
                    }

                    if (shouldClearAllDimensions) {
                        for (MapDimension dim : this.mapWorld.getDimensionsList()) {
                            if (!currentDimChecked || dim != currentDim) {
                                for (LeveledRegion<?> region : dim.getLayeredMapRegions().getUnsyncedSet()) {
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
                        for (MapDimension dim : this.mapWorld.getDimensionsList()) {
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
                    int totalLockAttempts = 10;
                    int lockAttempts = 10;

                    while (lockAttempts-- > 0) {
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

                this.checkFootstepsReset(this.world, this.newWorld);
                this.mapSaveLoad.clearToLoad();
                this.mapSaveLoad.setNextToLoadByViewing((LeveledRegion<?>) null);
                this.clearToRefresh();

                for (int i = 0; i < this.toProcessLevels.length; ++i) {
                    this.toProcessLevels[i].clear();
                }

                if (this.mapWorldUsable && !this.isCurrentMapLocked()) {
                    for(LeveledRegion<?> region : this.mapWorld.getCurrentDimension().getLayeredMapRegions().getUnsyncedSet()) {
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

                this.mapWorld.onWorldChangeUnsynced(this.world);
                if (WorldMap.settings.debug) {
                    WorldMap.LOGGER.info("World/dimension changed to: " + this.currentWorldId + " " + this.currentDimId + " " + this.currentMWId);
                }
                XaeroPlus.EVENT_BUS.post(new XaeroWorldChangeEvent(this.currentWorldId, this.currentDimId, this.currentMWId));

                this.worldDataHandler.prepareSingleplayer(this.world, (MapProcessor) (Object) this);
                if (this.worldDataHandler.getWorldDir() == null && this.currentWorldId != null && this.mapWorld.getCurrentDimension().isUsingWorldSave()) {
                    this.currentWorldId = this.currentDimId = null;
                }

                boolean shouldDetect = this.mapWorldUsable && !this.mapWorld.getCurrentDimension().hasDoneRegionDetection();
                this.mapSaveLoad.setRegionDetectionComplete(!shouldDetect);
                this.popRenderPause(true, true);
                this.popWriterPause();
            } else if (this.newWorld != this.world) {
                this.pushRenderPause(false, true);
                this.pushWriterPause();
                this.checkFootstepsReset(this.world, this.newWorld);
                this.world = this.newWorld;
                if (SupportMods.framedBlocks()) {
                    SupportMods.supportFramedBlocks.onWorldChange();
                }
                this.mapWorld.onWorldChangeUnsynced(this.world);
                this.popRenderPause(false, true);
                this.popWriterPause();
            }

            if (this.mapWorldUsable) {
                this.mapWorld.getCurrentDimension().switchToFutureMultiworldWritableValueUnsynced();
                this.mapWorld.switchToFutureMultiworldTypeUnsynced();
            }

            this.waitingForWorldUpdate = false;
        }
    }

    @Redirect(method = "onRenderProcess", at = @At(value = "INVOKE", target = "Lxaero/map/MapWriter;onRender(Lxaero/map/biome/BiomeColorCalculator;Lxaero/map/region/OverlayManager;)V"))
    public void redirectOnRenderProcess(final MapWriter instance, final BiomeColorCalculator biomeColorCalculator, final OverlayManager overlayManager) {
        if (XaeroPlusSettingRegistry.fastMapSetting.getValue()) return;
        instance.onRender(biomeColorCalculator, overlayManager);
    }

    @Inject(method = "onClientTickStart", at = @At("RETURN"))
    public void onClientTickStartReturn(final CallbackInfo ci) {
        if (!XaeroPlusSettingRegistry.fastMapSetting.getValue()) return;
        this.mapWriter.onRender(this.biomeColorCalculator, this.overlayManager);
    }
}
