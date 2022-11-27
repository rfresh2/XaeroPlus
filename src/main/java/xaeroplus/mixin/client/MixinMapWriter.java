package xaeroplus.mixin.client;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockGlass;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import xaero.map.MapProcessor;
import xaero.map.MapWriter;
import xaero.map.biome.WriterBiomeInfoSupplier;
import xaero.map.cache.BlockStateColorTypeCache;
import xaero.map.misc.Misc;
import xaero.map.region.MapBlock;
import xaero.map.region.OverlayBuilder;

@Mixin(value = MapWriter.class, remap = false)
public abstract class MixinMapWriter {
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

    protected MixinMapWriter() {
    }

    @Shadow
    protected abstract boolean shouldOverlayCached(IBlockState state);

    @Shadow
    public abstract boolean hasVanillaColor(IBlockState state, World world, BlockPos pos);

    @Shadow
    public abstract boolean isInvisible(World world, IBlockState state, Block b, boolean flowers);

    @Shadow
    public abstract boolean isGlowing(IBlockState state);

    /**
     * @author Entropy5
     * @reason obsidian roof
     */
    @Overwrite
    public boolean shouldOverlay(IBlockState state) {
        if (!(state.getBlock() instanceof BlockAir) && !(state.getBlock() instanceof BlockGlass) && state.getBlock().getBlockLayer() != BlockRenderLayer.TRANSLUCENT) {
            if (!(state.getBlock() instanceof BlockLiquid)) {
                return false;
            } else {
                int lightOpacity = state.getLightOpacity(this.mapProcessor.getWorld(), BlockPos.ORIGIN);
                return lightOpacity != 255 && lightOpacity != 0;
            }
        } else {
            return true;
        }
    }

    /**
     * @author Entropy5
     * @reason obsidian roof
     */
    @Overwrite
    public void loadPixel(World world, MapBlock pixel, MapBlock currentPixel, Chunk bchunk, int insideX, int insideZ, int highY, int lowY, boolean cave, boolean canReuseBiomeColours, boolean flowers, BlockPos.MutableBlockPos mutableBlockPos3) {
        pixel.prepareForWriting();
        this.overlayBuilder.startBuilding();
        boolean underair = !cave;
        IBlockState opaqueState = null;
        byte workingLight = -1;
        this.topH = lowY;
        this.mutableGlobalPos.setPos((bchunk.getPos().x << 4) + insideX, lowY - 1, (bchunk.getPos().z << 4) + insideZ);
        boolean shouldExtendTillTheBottom = false;
        int transparentSkipY = 0;

        int h;
        IBlockState state;
        for (h = highY; h >= lowY; h = shouldExtendTillTheBottom ? transparentSkipY : h - 1) {
            this.mutableLocalPos.setPos(insideX, h, insideZ);
            state = bchunk.getBlockState(this.mutableLocalPos);
            shouldExtendTillTheBottom = !shouldExtendTillTheBottom && !this.overlayBuilder.isEmpty() && this.firstTransparentStateY - h >= 255;
            if (shouldExtendTillTheBottom) {
                for (transparentSkipY = h - 1; transparentSkipY >= lowY; --transparentSkipY) {
                    IBlockState traceState = bchunk.getBlockState(mutableBlockPos3.setPos(insideX, transparentSkipY, insideZ));
                    if (!this.shouldOverlayCached(traceState)) {
                        break;
                    }
                }
            }
            Block b = state.getBlock();
            boolean roofObsidian = (h > 253 && b == Blocks.OBSIDIAN);
            if (b instanceof BlockAir) {
                underair = true;
            } else if (underair) {
                this.mutableGlobalPos.setY(h);
                this.mutableLocalPos.setY(Math.min(255, h + 1));
                workingLight = (byte) bchunk.getLightFor(EnumSkyBlock.BLOCK, this.mutableLocalPos);
                if (!this.isInvisible(world, state, b, flowers)) {
                    if (this.shouldOverlayCached(state)  || roofObsidian) {
                        if (h > this.topH) {
                            this.topH = h;
                        }

                        if (this.overlayBuilder.isEmpty()) {
                            this.firstTransparentStateY = h;
                        }

                        if (shouldExtendTillTheBottom) {
                            this.overlayBuilder.getCurrentOverlay().increaseOpacity(Misc.getStateById(this.overlayBuilder.getCurrentOverlay().getState()).getLightOpacity(world, this.mutableGlobalPos) * (h - transparentSkipY));
                        } else {
                            this.writerBiomeInfoSupplier.set(currentPixel, canReuseBiomeColours);
                            int stateId = Block.getStateId(state);
                            int opacity = roofObsidian ? 50 : b.getLightOpacity(state, world, this.mutableGlobalPos);
                            this.overlayBuilder.build(stateId, this.biomeBuffer, opacity, workingLight, world, this.mapProcessor, this.mutableGlobalPos, this.overlayBuilder.getOverlayBiome(), this.colorTypeCache, this.writerBiomeInfoSupplier);
                        }
                    } else if (this.hasVanillaColor(state, world, this.mutableGlobalPos)) {
                        if (h > this.topH) {
                            this.topH = h;
                        }

                        opaqueState = state;
                        break;
                    }
                }
            }
        }

        if (h < lowY) {
            h = lowY;
        }

        state = opaqueState == null ? Blocks.AIR.getDefaultState() : opaqueState;
        int stateId = Block.getStateId(state);
        byte light = opaqueState == null ? 0 : workingLight;
        this.overlayBuilder.finishBuilding(pixel);
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


}
