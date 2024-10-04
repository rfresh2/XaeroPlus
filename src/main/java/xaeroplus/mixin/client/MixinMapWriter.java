package xaeroplus.mixin.client;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalLongRef;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockGlass;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.MapProcessor;
import xaero.map.MapWriter;
import xaero.map.biome.BiomeColorCalculator;
import xaero.map.biome.WriterBiomeInfoSupplier;
import xaero.map.cache.BlockStateColorTypeCache;
import xaero.map.misc.Misc;
import xaero.map.region.MapBlock;
import xaero.map.region.MapRegion;
import xaero.map.region.OverlayBuilder;
import xaero.map.region.OverlayManager;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.util.ChunkUtils;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.nonNull;

@Mixin(value = MapWriter.class, remap = false)
public abstract class MixinMapWriter {
    @Shadow
    private int playerChunkX;
    @Shadow
    private int playerChunkZ;
    @Shadow
    private OverlayBuilder overlayBuilder;
    @Shadow
    private int topH;
    @Shadow
    private int firstTransparentStateY;
    @Shadow
    private WriterBiomeInfoSupplier writerBiomeInfoSupplier;
    @Shadow
    private int[] biomeBuffer;
    @Shadow
    private BlockStateColorTypeCache colorTypeCache;
    @Shadow
    private MapProcessor mapProcessor;
    @Shadow
    @Final
    private BlockPos.MutableBlockPos mutableLocalPos;
    @Shadow
    @Final
    private BlockPos.MutableBlockPos mutableGlobalPos;
    @Shadow
    private int endTileChunkX;
    @Shadow
    private int endTileChunkZ;
    @Shadow
    private int startTileChunkX;
    @Shadow
    private int startTileChunkZ;
    @Shadow
    private long lastWriteTry;
    @Shadow
    private long lastWrite;
    @Shadow
    private int writeFreeSizeTiles;
    @Shadow
    private int writeFreeFullUpdateTargetTime;
    @Shadow
    private int workingFrameCount;
    @Shadow
    private long framesFreedTime = -1L;
    @Shadow
    public long writeFreeSinceLastWrite;
    @Final
    @Shadow
    private BlockPos.MutableBlockPos mutableBlockPos3;
    @Shadow
    private ArrayList<MapRegion> regionBuffer;
    @Shadow
    private int writingLayer;

    @Shadow
    public abstract boolean writeMap(
            World world,
            double playerX,
            double playerY,
            double playerZ,
            BiomeColorCalculator biomeColorCalculator,
            OverlayManager overlayManager,
            boolean loadChunks,
            boolean updateChunks,
            boolean ignoreHeightmaps,
            boolean flowers,
            boolean detailedDebug,
            BlockPos.MutableBlockPos mutableBlockPos3,
            int caveDepth
    );

    protected MixinMapWriter() {
    }

    @Shadow
    protected abstract boolean shouldOverlayCached(IBlockState state);

    @Shadow
    public abstract boolean hasVanillaColor(IBlockState state, World world, BlockPos pos);

    @Shadow
    public abstract boolean isInvisible(IBlockState state, Block b, boolean flowers);

    @Shadow
    public abstract boolean isGlowing(IBlockState state);

    @Shadow
    protected abstract IBlockState unpackFramedBlocks(IBlockState original, World world, BlockPos globalPos);

    // todo: rewrite obsidian roof mixins without overwrites

    /**
     * @author Entropy5
     * @reason obsidian roof
     */
    @Inject(method = "shouldOverlay", at = @At("HEAD"), cancellable = true)
    public void shouldOverlay(IBlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (!XaeroPlusSettingRegistry.transparentObsidianRoofSetting.getValue()) {
            return;
        }
        if (!(state.getBlock() instanceof BlockAir) && !(state.getBlock() instanceof BlockGlass) && state.getBlock().getRenderLayer() != BlockRenderLayer.TRANSLUCENT) {
            if (!(state.getBlock() instanceof BlockLiquid)) {
                cir.setReturnValue(false);
            } else {
                int lightOpacity = state.getLightOpacity(this.mapProcessor.getWorld(), BlockPos.ORIGIN);
                cir.setReturnValue(lightOpacity != 0); // deleted argument to render water under obsidian roof regardless of light opacity
            }
        } else {
            cir.setReturnValue(true);
        }
    }

    /**
     * @author Entropy5
     * @reason obsidian roof
     */
    @Inject(method = "loadPixel", at = @At("HEAD"), cancellable = true)
    public void loadPixel(World world, MapBlock pixel, MapBlock currentPixel,
                          Chunk bchunk, int insideX, int insideZ,
                          int highY, int lowY, boolean cave,
                          boolean fullCave,
                          int mappedHeight,
                          boolean canReuseBiomeColours,
                          boolean ignoreHeightmaps,
                          boolean flowers,
                          BlockPos.MutableBlockPos mutableBlockPos3,
                          CallbackInfo ci) {
        if (!XaeroPlusSettingRegistry.transparentObsidianRoofSetting.getValue()) {
            return;
        } else {
            ci.cancel();
        }
        pixel.prepareForWriting();
        this.overlayBuilder.startBuilding();
        boolean underair = !cave || fullCave;
        boolean shouldEnterGround = fullCave;
        IBlockState opaqueState = null;
        byte workingLight = -1;
        boolean worldHasSkyLight = world.provider.hasSkyLight();
        byte workingSkyLight = (byte)(worldHasSkyLight ? 15 : 0);
        this.topH = lowY;
        this.mutableGlobalPos.setPos((bchunk.getPos().x << 4) + insideX, lowY - 1, (bchunk.getPos().z << 4) + insideZ);
        boolean shouldExtendTillTheBottom = false;
        int transparentSkipY = 0;
        boolean columnRoofObsidian = false;

        // todo: figure out if this still works

        int h;
        IBlockState state;
        for (h = highY; h >= lowY; h = shouldExtendTillTheBottom ? transparentSkipY : h - 1) {
            this.mutableLocalPos.setPos(insideX, h, insideZ);
            this.mutableGlobalPos.setY(h);
            state = bchunk.getBlockState(this.mutableLocalPos);
            if (state == null) {
                state = Blocks.AIR.getDefaultState();
            }
            state = this.unpackFramedBlocks(state, world, this.mutableGlobalPos);
            Block b = state.getBlock();
            boolean roofObsidian = (h > 253 && b == Blocks.OBSIDIAN);
            if (roofObsidian && XaeroPlusSettingRegistry.transparentObsidianRoofDarkeningSetting.getValue() == 0) {
                continue;  // skip over obsidian roof completely
            }
            if (roofObsidian & !columnRoofObsidian) {
                columnRoofObsidian = true;
            }
            shouldExtendTillTheBottom = !shouldExtendTillTheBottom && !this.overlayBuilder.isEmpty() && this.firstTransparentStateY - h >= 5 && !columnRoofObsidian;
            if (shouldExtendTillTheBottom) {
                for (transparentSkipY = h - 1; transparentSkipY >= lowY; --transparentSkipY) {
                    IBlockState traceState = bchunk.getBlockState(mutableBlockPos3.setPos(insideX, transparentSkipY, insideZ));
                    if (traceState == null) { // should be impossible lol
                        traceState = Blocks.AIR.getDefaultState();
                    }
                    if (!this.shouldOverlayCached(traceState)) {
                        break;
                    }
                }
            }
            if (b instanceof BlockAir) {
                underair = true;
            } else if (underair && !this.isInvisible(state, b, flowers)) {
                if (!cave || !shouldEnterGround) {
                    this.mutableLocalPos.setY(Math.min(255, h + 1));
                    workingLight = (byte)bchunk.getLightFor(EnumSkyBlock.BLOCK, this.mutableLocalPos);
                    if (cave && workingLight < 15 && worldHasSkyLight) {
                        if (!ignoreHeightmaps && !fullCave && highY >= mappedHeight) {
                            workingSkyLight = 15;
                        } else {
                            workingSkyLight = (byte)bchunk.getLightFor(EnumSkyBlock.SKY, this.mutableLocalPos);
                        }
                    }
                    if (this.shouldOverlayCached(state) || roofObsidian) {
                        if (h > this.topH) {
                            this.topH = h;
                        }

                        byte overlayLight = workingLight;
                        if (this.overlayBuilder.isEmpty()) {
                            this.firstTransparentStateY = h;
                            if (cave && workingSkyLight > workingLight) {
                                overlayLight = workingSkyLight;
                            }
                        }

                        if (shouldExtendTillTheBottom) {
                            this.overlayBuilder.getCurrentOverlay().increaseOpacity(Misc.getStateById(this.overlayBuilder.getCurrentOverlay().getState()).getLightOpacity(world, this.mutableGlobalPos) * (h - transparentSkipY));
                        } else {
                            this.writerBiomeInfoSupplier.set(currentPixel, canReuseBiomeColours);
                            int stateId = Block.getStateId(state);
                            int opacity = roofObsidian ? 5 : b.getLightOpacity(state, world, this.mutableGlobalPos);
                            this.overlayBuilder.build(stateId, this.biomeBuffer, opacity, overlayLight, world, this.mapProcessor, this.mutableGlobalPos, this.overlayBuilder.getOverlayBiome(), this.colorTypeCache, this.writerBiomeInfoSupplier);
                        }
                    } else if (this.hasVanillaColor(state, world, this.mutableGlobalPos)) {
                        if (h > this.topH) {
                            this.topH = h;
                        }

                        opaqueState = state;
                        break;
                    }
                } else if (!state.getMaterial().getCanBurn()
                        && !state.getMaterial().isReplaceable()
                        && state.getMaterial().getPushReaction() != EnumPushReaction.DESTROY
                        && !this.shouldOverlayCached(state)) {
                    underair = false;
                    shouldEnterGround = false;
                }
            }
        }

        if (h < lowY) {
            h = lowY;
        }

        state = opaqueState == null ? Blocks.AIR.getDefaultState() : opaqueState;
        int stateId = Block.getStateId(state);
        this.overlayBuilder.finishBuilding(pixel);
        byte light = 0;
        if (opaqueState != null) {
            light = workingLight;
            if (cave && workingLight < 15 && pixel.getNumberOfOverlays() == 0 && workingSkyLight > workingLight) {
                light = workingSkyLight;
            }
        } else {
            h = 0;
        }
        if (canReuseBiomeColours && currentPixel != null && currentPixel.getState() == stateId) {
            this.biomeBuffer[0] = currentPixel.getColourType();
            this.biomeBuffer[1] = currentPixel.getBiome();
            this.biomeBuffer[2] = currentPixel.getCustomColour();
        } else {
            this.colorTypeCache.getBlockBiomeColour(world, state, this.mutableGlobalPos, this.biomeBuffer, -1);
        }

        if (this.overlayBuilder.getOverlayBiome() != -1) {
            this.biomeBuffer[1] = this.overlayBuilder.getOverlayBiome();
        }

        boolean glowing = this.isGlowing(state);
        pixel.write(stateId, h, this.topH, this.biomeBuffer, light, glowing, cave);
    }

    @Inject(method = "onRender", at = @At(
        value = "FIELD",
        target = "Lxaero/map/MapWriter;lastWrite:J",
        opcode = Opcodes.GETFIELD,
        ordinal = 2
    ))
    public void fastMapMaxTilesPerCycleSetting(final BiomeColorCalculator biomeColorCalculator, final OverlayManager overlayManager, final CallbackInfo ci,
                                               @Local(name = "tilesToUpdate") LocalLongRef tilesToUpdateRef,
                                               @Local(name = "sizeTiles") int sizeTiles) {
        if (XaeroPlusSettingRegistry.fastMapSetting.getValue()) {
            this.writeFreeSinceLastWrite = Math.max(1L, this.writeFreeSinceLastWrite);
            if (this.mapProcessor.getCurrentCaveLayer() == Integer.MAX_VALUE) {
                tilesToUpdateRef.set((long) Math.min(sizeTiles, XaeroPlusSettingRegistry.fastMapMaxTilesPerCycle.getValue()));
            }
        }
    }

    @ModifyExpressionValue(method = "onRender", at = @At(
        value = "INVOKE",
        target = "Ljava/lang/System;nanoTime()J",
        ordinal = 2
    ))
    public long removeWriteTimeLimiterPerFrame(long original) {
        if (XaeroPlusSettingRegistry.fastMapSetting.getValue()) {
            if (this.mapProcessor.getCurrentCaveLayer() == Integer.MAX_VALUE) {
                return 0;
            }
        }
        return original;
    }

    // insert our own limiter on new tiles being written but this one's keyed on the actual chunk
    // tile "writes" also include a lot of extra operations and lookups before any writing is actually done
    // when we remove existing limiters those extra operations add up to a lot of unnecessary cpu time
    private final Cache<Long, Long> tileUpdateCache = Caffeine.newBuilder()
        // I would usually expect even second long expiration here to be fine
        // but there are some operations that make repeat invocations actually required
        // perhaps another time ill rewrite those. Or make the cache lock more aware of when we don't have any new updates to write/load
        // there's still alot of performance and efficiency on the table to regain
        // but i think this is a good middle ground for now
        .maximumSize(10000)
        .expireAfterWrite(5L, TimeUnit.SECONDS)
        .<Long, Long>build();

    @WrapOperation(method = "writeMap", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/MapWriter;writeChunk(Lnet/minecraft/world/World;IZLxaero/map/biome/BiomeColorCalculator;Lxaero/map/region/OverlayManager;ZZZZZLnet/minecraft/util/math/BlockPos$MutableBlockPos;IIIIIIIII)Z"
    ))
    public boolean fastMap(final MapWriter instance,
                           World world,
                           int distance,
                           boolean onlyLoad,
                           BiomeColorCalculator biomeColorCalculator,
                           OverlayManager overlayManager,
                           boolean loadChunks,
                           boolean updateChunks,
                           boolean ignoreHeightmaps,
                           boolean flowers,
                           boolean detailedDebug,
                           BlockPos.MutableBlockPos mutableBlockPos3,
                           int caveDepth,
                           int caveStart,
                           int layerToWrite,
                           int tileChunkX,
                           int tileChunkZ,
                           int tileChunkLocalX,
                           int tileChunkLocalZ,
                           int chunkX,
                           int chunkZ,
                           final Operation<Boolean> original) {
        if (XaeroPlusSettingRegistry.fastMapSetting.getValue()) {
            if (this.mapProcessor.getCurrentCaveLayer() == Integer.MAX_VALUE) {
                final Long cacheable = ChunkUtils.chunkPosToLong(chunkX, chunkZ);
                final Long cacheValue = tileUpdateCache.getIfPresent(cacheable);
                if (nonNull(cacheValue)) {
                    if (cacheValue < System.currentTimeMillis() - 50L) {
                        tileUpdateCache.put(cacheable, System.currentTimeMillis());
                    } else {
                        return false;
                    }
                } else {
                    tileUpdateCache.put(cacheable, System.currentTimeMillis());
                }
            }
        }
        return original.call(instance, world, distance, onlyLoad, biomeColorCalculator, overlayManager, loadChunks, updateChunks,
                             ignoreHeightmaps, flowers, detailedDebug, mutableBlockPos3, caveDepth, caveStart, layerToWrite, tileChunkX,
                             tileChunkZ, tileChunkLocalX, tileChunkLocalZ, chunkX, chunkZ);
    }

    @Redirect(method = "writeChunk", at = @At(value = "INVOKE", target = "Lxaero/map/MapWriter;loadPixel(Lnet/minecraft/world/World;Lxaero/map/region/MapBlock;Lxaero/map/region/MapBlock;Lnet/minecraft/world/chunk/Chunk;IIIIZZIZZZLnet/minecraft/util/math/BlockPos$MutableBlockPos;)V"))
    public void redirectLoadPixelForNetherFix(MapWriter instance, World world,
                                              MapBlock pixel,
                                              MapBlock currentPixel,
                                              Chunk bchunk,
                                              int insideX,
                                              int insideZ,
                                              int highY,
                                              int lowY,
                                              boolean cave,
                                              boolean fullCave,
                                              int mappedHeight,
                                              boolean canReuseBiomeColours,
                                              boolean ignoreHeightmaps,
                                              boolean flowers,
                                              BlockPos.MutableBlockPos mutableBlockPos3) {
        if (XaeroPlusSettingRegistry.netherCaveFix.getValue()) {
            final boolean nether = world.provider.getDimensionType() == DimensionType.NETHER;
            final boolean shouldForceFullInNether = !cave && nether;
            instance.loadPixel(world, pixel, currentPixel, bchunk, insideX, insideZ, highY, lowY,
                    shouldForceFullInNether || cave,
                    shouldForceFullInNether || fullCave,
                    mappedHeight, canReuseBiomeColours, ignoreHeightmaps, flowers, mutableBlockPos3);
        } else {
            instance.loadPixel(world, pixel, currentPixel, bchunk, insideX, insideZ, highY, lowY, cave, fullCave, mappedHeight, canReuseBiomeColours, ignoreHeightmaps, flowers, mutableBlockPos3);
        }
    }
}
