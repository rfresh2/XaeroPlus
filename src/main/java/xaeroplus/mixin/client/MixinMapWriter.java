package xaeroplus.mixin.client;

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
import xaero.map.biome.WriterBiomeInfoSupplier;
import xaero.map.cache.BlockStateColorTypeCache;
import xaero.map.misc.Misc;
import xaero.map.region.MapBlock;
import xaero.map.region.MapRegion;
import xaero.map.region.OverlayBuilder;
import xaeroplus.settings.Settings;

import java.util.ArrayList;

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
        if (!Settings.REGISTRY.transparentObsidianRoofSetting.getValue()) {
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
        if (!Settings.REGISTRY.transparentObsidianRoofSetting.getValue()) {
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
            if (roofObsidian && Settings.REGISTRY.transparentObsidianRoofDarkeningSetting.getValue() == 0) {
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
        if (Settings.REGISTRY.netherCaveFix.getValue()) {
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
