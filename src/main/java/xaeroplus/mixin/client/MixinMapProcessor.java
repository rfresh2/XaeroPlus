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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.deallocator.ByteBufferDeallocator;
import xaero.map.*;
import xaero.map.biome.BiomeColorCalculator;
import xaero.map.biome.MapBiomes;
import xaero.map.cache.BlockStateColorTypeCache;
import xaero.map.cache.BlockStateShortShapeCache;
import xaero.map.file.MapSaveLoad;
import xaero.map.file.worldsave.WorldDataHandler;
import xaero.map.graphics.TextureUploader;
import xaero.map.gui.GuiMap;
import xaero.map.gui.message.MessageBox;
import xaero.map.gui.message.render.MessageBoxRenderer;
import xaero.map.highlight.HighlighterRegistry;
import xaero.map.highlight.MapRegionHighlightsPreparer;
import xaero.map.pool.MapTilePool;
import xaero.map.region.LeveledRegion;
import xaero.map.region.OverlayManager;
import xaero.map.region.texture.BranchTextureRenderer;
import xaero.map.world.MapWorld;
import xaeroplus.XaeroPlus;

import java.util.ArrayList;

import static java.util.Objects.nonNull;

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

    @Inject(method = "getMainId", at = @At("HEAD"), cancellable = true)
    private void getMainId(boolean rootFolderFormat, CallbackInfoReturnable<String> cir) {
        Minecraft mc = Minecraft.getMinecraft();
        if (nonNull(mc.getCurrentServerData())) {
            // use common directories based on server list name instead of IP
            // good for proxies
            cir.setReturnValue("Multiplayer_" + mc.getCurrentServerData().serverName);
            cir.cancel();
        }
    }

    @Inject(method = "<init>", at = @At(value = "TAIL"))
    public void init(MapSaveLoad mapSaveLoad, MapWriter mapWriter, MapLimiter mapLimiter, ByteBufferDeallocator bufferDeallocator, MapTilePool tilePool, OverlayManager overlayManager, TextureUploader textureUploader, WorldDataHandler worldDataHandler, MapBiomes mapBiomes, BranchTextureRenderer branchTextureRenderer, BiomeColorCalculator biomeColorCalculator, BlockStateColorTypeCache blockStateColorTypeCache, BlockStateShortShapeCache blockStateShortShapeCache, HighlighterRegistry highlighterRegistry, MapRegionHighlightsPreparer mapRegionHighlightsPreparer, MessageBox messageBox, MessageBoxRenderer messageBoxRenderer, CallbackInfo ci) throws NoSuchFieldException {
        // Use variable to select cache level instead of hardcoded value
        toProcessLevels = new ArrayList[XaeroPlus.MAX_LEVEL + 1];
        for (int i = 0; i < this.toProcessLevels.length; ++i) {
            this.toProcessLevels[i] = new ArrayList();
        }
    }

    /**
     * @author rfresh2
     * @reason Use DIM0 as overworld region directory name instead of "null"
     */
    @Overwrite
    public String getDimensionName(int id) {
        return "DIM" + id; // remove backwards compatibility for "null" overworld dimension id
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
}
