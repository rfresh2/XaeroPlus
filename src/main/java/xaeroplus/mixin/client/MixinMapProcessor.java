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
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.MapLimiter;
import xaero.map.MapProcessor;
import xaero.map.MapRunner;
import xaero.map.WorldMap;
import xaero.map.biome.BiomeGetter;
import xaero.map.file.MapSaveLoad;
import xaero.map.gui.GuiMap;
import xaero.map.region.LeveledRegion;
import xaero.map.world.MapWorld;
import xaeroplus.XaeroPlus;
import xaeroplus.util.DataFolderResolveUtil;
import xaeroplus.util.Shared;

import java.io.IOException;
import java.util.ArrayList;

@Mixin(value = MapProcessor.class, remap = false)
public abstract class MixinMapProcessor {
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

    @Inject(method = "getMainId", at = @At("HEAD"), cancellable = true)
    private void getMainId(final boolean rootFolderFormat, final ClientPlayNetworkHandler connection, final CallbackInfoReturnable<String> cir) {
        DataFolderResolveUtil.resolveDataFolder(connection, cir);
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

    @Inject(method = "getDimensionName", at = @At(value = "HEAD"), cancellable = true)
    public void getDimensionName(final RegistryKey<World> id, final CallbackInfoReturnable<String> cir) {
        if (!Shared.nullOverworldDimensionFolder) {
            if (id == World.OVERWORLD) {
                cir.setReturnValue("DIM0");
            }
        }
    }
}
