package xaeroplus.mixin.client;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.llamalad7.mixinextras.sugar.ref.LocalLongRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.MapProcessor;
import xaero.map.MapWriter;
import xaero.map.biome.BiomeColorCalculator;
import xaero.map.biome.BlockTintProvider;
import xaero.map.region.MapBlock;
import xaero.map.region.OverlayBuilder;
import xaero.map.region.OverlayManager;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.util.ChunkUtils;

import java.util.concurrent.TimeUnit;

import static java.util.Objects.nonNull;
import static net.minecraft.world.level.Level.NETHER;

@Mixin(value = MapWriter.class, remap = false)
public abstract class MixinMapWriter {
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

    @Shadow
    private MapProcessor mapProcessor;
    @Shadow
    @Final
    private BlockPos.MutableBlockPos mutableLocalPos;

    @Inject(method = "loadPixel", at = @At("HEAD"), remap = true)
    public void setObsidianColumnLocalVar(final Level world, final Registry<Block> blockRegistry, final MapBlock pixel, final MapBlock currentPixel, final LevelChunk bchunk, final int insideX, final int insideZ, final int highY, final int lowY, final boolean cave, final boolean fullCave, final int mappedHeight, final boolean canReuseBiomeColours, final boolean ignoreHeightmaps, final Registry<Biome> biomeRegistry, final boolean flowers, final int worldBottomY, final BlockPos.MutableBlockPos mutableBlockPos3, final CallbackInfo ci,
                                          @Share("columnRoofObsidian") LocalBooleanRef columnRoofObsidianRef) {
        if (!XaeroPlusSettingRegistry.transparentObsidianRoofSetting.getValue()) return;
        columnRoofObsidianRef.set(false);
    }

    @Inject(method = "loadPixel", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/world/level/block/state/BlockState;getFluidState()Lnet/minecraft/world/level/material/FluidState;",
        ordinal = 0
    ), remap = true)
    public void obsidianRoofHeadInject(final Level world, final Registry<Block> blockRegistry, final MapBlock pixel, final MapBlock currentPixel, final LevelChunk bchunk, final int insideX, final int insideZ, final int highY, final int lowY, final boolean cave, final boolean fullCave, final int mappedHeight, final boolean canReuseBiomeColours, final boolean ignoreHeightmaps, final Registry<Biome> biomeRegistry, final boolean flowers, final int worldBottomY, final BlockPos.MutableBlockPos mutableBlockPos3, final CallbackInfo ci,
                                       @Local(name = "state") LocalRef<BlockState> stateRef,
                                       @Local(name = "h") LocalIntRef hRef,
                                       @Local(name = "transparentSkipY") LocalIntRef transparentSkipYRef,
                                       @Share("columnRoofObsidian") LocalBooleanRef columnRoofObsidianRef
    ) {
        if (!XaeroPlusSettingRegistry.transparentObsidianRoofSetting.getValue()) return;
        final Block b = stateRef.get().getBlock();
        final boolean blockHeightAboveYLimit = hRef.get() >= XaeroPlusSettingRegistry.transparentObsidianRoofYSetting.getValue();

        if (blockHeightAboveYLimit) {
            boolean shouldMakeTransparent = (b == Blocks.OBSIDIAN);
            if (b == Blocks.SNOW) {
                this.mutableLocalPos.setY(hRef.get() - 1);
                BlockState belowState = bchunk.getBlockState(this.mutableLocalPos);
                this.mutableLocalPos.setY(hRef.get());
                shouldMakeTransparent = belowState.getBlock() == Blocks.OBSIDIAN;
            }
            if (shouldMakeTransparent) {
                if (XaeroPlusSettingRegistry.transparentObsidianRoofDarkeningSetting.getValue() == 0) {
                    stateRef.set(Blocks.AIR.defaultBlockState());
                    transparentSkipYRef.set(transparentSkipYRef.get() - 1);
                }
                if (!columnRoofObsidianRef.get()) columnRoofObsidianRef.set(true);
            }
        }
    }

    // What we want: a = !b && !c
    // But we're wrapping b
    // so instead we do a = !(b || c)
    @WrapOperation(method = "loadPixel", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/region/OverlayBuilder;isEmpty()Z"
    ), remap = true)
    public boolean checkObsidianRoofColumn(final OverlayBuilder instance, final Operation<Boolean> original,
                                           @Share("columnRoofObsidian") final LocalBooleanRef columnRoofObsidianRef) {
        if (!XaeroPlusSettingRegistry.transparentObsidianRoofSetting.getValue()) return original.call(instance);
        return original.call(instance) || columnRoofObsidianRef.get();
    }

    @ModifyExpressionValue(method = "loadPixelHelp", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/MapWriter;shouldOverlayCached(Lnet/minecraft/world/level/block/state/StateHolder;)Z",
        ordinal = 0
    ), remap = true)
    public boolean obsidianRoofOverlayMod(final boolean original,
                                          @Local(argsOnly = true) LevelChunk bChunk,
                                          @Local(name = "b") Block b,
                                          @Local(name = "h") int h
    ) {
        if (XaeroPlusSettingRegistry.transparentObsidianRoofSetting.getValue()
            && h > XaeroPlusSettingRegistry.transparentObsidianRoofYSetting.getValue()) {
            if (b == Blocks.OBSIDIAN) {
                return true;
            } else if (b == Blocks.SNOW) {
                this.mutableLocalPos.setY(h - 1);
                BlockState belowState = bChunk.getBlockState(this.mutableLocalPos);
                this.mutableLocalPos.setY(h);
                return belowState.getBlock() == Blocks.OBSIDIAN;
            }
        }
        return original;
    }

    @WrapOperation(method = "loadPixelHelp", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/world/level/block/state/BlockState;getLightBlock(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)I",
        ordinal = 1
    ), remap = true)
    public int getOpacityForObsidianRoof(BlockState instance, BlockGetter world, BlockPos pos, Operation<Integer> original,
                                         @Local(name = "h") int h) {
        if (XaeroPlusSettingRegistry.transparentObsidianRoofSetting.getValue()
            && h > XaeroPlusSettingRegistry.transparentObsidianRoofYSetting.getValue()) {
            boolean shouldMakeTransparent = instance.getBlock() == Blocks.OBSIDIAN;
            if (instance.getBlock() == Blocks.SNOW) {
                this.mutableLocalPos.setY(h - 1);
                BlockState belowState = world.getBlockState(this.mutableLocalPos);
                this.mutableLocalPos.setY(h);
                if (belowState.getBlock() == Blocks.OBSIDIAN)
                    shouldMakeTransparent = true;
            }
            if (shouldMakeTransparent) {
                return 5;
            }
        }
        return original.call(instance, world, pos);
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
        if (!(XaeroPlusSettingRegistry.fastMapSetting.getValue() && this.mapProcessor.getCurrentCaveLayer() == Integer.MAX_VALUE)) return; // do default logic when fast mapping is off or we're mapping caves
        tilesToUpdateRef.set((long) Math.min(sizeTiles, XaeroPlusSettingRegistry.fastMapMaxTilesPerCycle.getValue()));
    }

    @ModifyExpressionValue(method = "onRender", at = @At(
        value = "INVOKE",
        target = "Ljava/lang/System;nanoTime()J",
        ordinal = 2
    ))
    public long removeWriteTimeLimiterPerFrame(long original) {
        if (!(XaeroPlusSettingRegistry.fastMapSetting.getValue() && this.mapProcessor.getCurrentCaveLayer() == Integer.MAX_VALUE)) return original;
        return 0;
    }

    @Inject(method = "writeChunk", at = @At(value = "HEAD"), cancellable = true, remap = true)
    public void writeChunk(
            Level world,
            Registry<Block> blockRegistry,
            int distance,
            boolean onlyLoad,
            Registry<Biome> biomeRegistry,
            OverlayManager overlayManager,
            boolean loadChunks,
            boolean updateChunks,
            boolean ignoreHeightmaps,
            boolean flowers,
            boolean detailedDebug,
            BlockPos.MutableBlockPos mutableBlockPos3,
            BlockTintProvider blockTintProvider,
            int caveDepth,
            int caveStart,
            int layerToWrite,
            int tileChunkX,
            int tileChunkZ,
            int tileChunkLocalX,
            int tileChunkLocalZ,
            int chunkX,
            int chunkZ,
            final CallbackInfoReturnable<Boolean> cir) {
        if (!XaeroPlusSettingRegistry.fastMapSetting.getValue()) return;

        final Long cacheable = ChunkUtils.chunkPosToLong(chunkX, chunkZ);
        final Long cacheValue = tileUpdateCache.getIfPresent(cacheable);
        if (nonNull(cacheValue)) {
            if (cacheValue < System.currentTimeMillis() - (long) XaeroPlusSettingRegistry.fastMapWriterDelaySetting.getValue()) {
                tileUpdateCache.put(cacheable, System.currentTimeMillis());
            } else {
                cir.setReturnValue(false);
                cir.cancel();
            }
        } else {
            tileUpdateCache.put(cacheable, System.currentTimeMillis());
        }
    }

    @Inject(method = "loadPixel", at = @At("HEAD"), remap = true)
    public void netherCaveFixInject(final Level world, final Registry<Block> blockRegistry, final MapBlock pixel, final MapBlock currentPixel, final LevelChunk bchunk, final int insideX, final int insideZ, final int highY, final int lowY, final boolean cave, final boolean fullCave, final int mappedHeight, final boolean canReuseBiomeColours, final boolean ignoreHeightmaps, final Registry<Biome> biomeRegistry, final boolean flowers, final int worldBottomY, final BlockPos.MutableBlockPos mutableBlockPos3, final CallbackInfo ci,
                                    @Local(index = 10, argsOnly = true) LocalBooleanRef caveRef,
                                    @Local(index = 11, argsOnly = true) LocalBooleanRef fullCaveRef
                                    ) {
        if (XaeroPlusSettingRegistry.netherCaveFix.getValue()) {
            var nether = world.dimension() == NETHER;
            var shouldForceFullInNether = !cave && nether;
            caveRef.set(shouldForceFullInNether || cave);
            fullCaveRef.set(shouldForceFullInNether || fullCave);
        }
    }
}
