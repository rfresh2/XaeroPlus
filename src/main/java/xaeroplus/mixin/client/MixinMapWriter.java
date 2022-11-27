package xaeroplus.mixin.client;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumBlockRenderType;
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
import xaero.map.WorldMap;
import xaero.map.biome.WriterBiomeInfoSupplier;
import xaero.map.cache.BlockStateColorTypeCache;
import xaero.map.cache.BlockStateShortShapeCache;
import xaero.map.file.MapSaveLoad;
import xaero.map.misc.CachedFunction;
import xaero.map.misc.Misc;
import xaero.map.region.MapBlock;
import xaero.map.region.OverlayBuilder;

import java.util.ArrayList;

@Mixin(value = MapWriter.class, remap = false)
public abstract class MixinMapWriter {
    @Shadow
    private ArrayList<IBlockState> buggedStates;
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

    /**
     * @author Entropy5
     * @reason obsidian roof
     */
    @Overwrite
    public boolean isInvisible(World world, IBlockState state, Block b, boolean flowers) {
        if (state.getRenderType() == EnumBlockRenderType.INVISIBLE) {
            return true;
        } else if (b == Blocks.TORCH) {
            return true;
        } else if (b == Blocks.TALLGRASS) {
            return true;
        } else if (b != Blocks.GLASS && b != Blocks.GLASS_PANE) {
            if (b == Blocks.DOUBLE_PLANT) {
                return true;
            } else if ((b instanceof BlockFlower || b instanceof BlockDoublePlant) && !flowers) {
                return true;
            } else {
                synchronized (this.buggedStates) {
                    return this.buggedStates.contains(state);
                }
            }
        } else {
            return true;
        }
    }

    @Shadow
    public abstract boolean isGlowing(IBlockState state);

    /**
     * @author Entropy5
     * @reason obsidian roof
     */
    @Overwrite
    public void loadPixel(World world, MapBlock pixel, MapBlock currentPixel, Chunk bchunk, int insideX, int insideZ, int highY, int lowY, boolean cave, boolean canReuseBiomeColours, boolean flowers, BlockPos.MutableBlockPos mutableBlockPos3) {
        pixel.prepareForWriting();
        this.overlayBuilder.startBuilding();
        IBlockState prevOverlay = null;
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
            shouldExtendTillTheBottom = !shouldExtendTillTheBottom && !this.overlayBuilder.isEmpty() && this.firstTransparentStateY - h >= 5;
            if (shouldExtendTillTheBottom) {
                for (transparentSkipY = h - 1; transparentSkipY >= lowY; --transparentSkipY) {
                    IBlockState traceState = bchunk.getBlockState(mutableBlockPos3.setPos(insideX, transparentSkipY, insideZ));
                    if (!this.shouldOverlayCached(traceState)) {
                        break;
                    }
                }
            }

            Block b = state.getBlock();
            if (b instanceof BlockAir) {
                underair = true;
            } else if (underair) {
                this.mutableGlobalPos.setY(h);
                this.mutableLocalPos.setY(Math.min(255, h + 1));
                workingLight = (byte) bchunk.getLightFor(EnumSkyBlock.BLOCK, this.mutableLocalPos);
                if (!this.isInvisible(world, state, b, flowers)) {
                    if (this.shouldOverlayCached(state)) {
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
                            this.overlayBuilder.build(stateId, this.biomeBuffer, b.getLightOpacity(state, world, this.mutableGlobalPos), workingLight, world, this.mapProcessor, this.mutableGlobalPos, this.overlayBuilder.getOverlayBiome(), this.colorTypeCache, this.writerBiomeInfoSupplier);
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
