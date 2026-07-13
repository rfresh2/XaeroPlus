package xaeroplus.mixin.client;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xaero.map.MapProcessor;
import xaero.map.MapWriter;
import xaero.map.biome.WriterBiomeInfoSupplier;
import xaero.map.cache.BlockStateColorTypeCache;
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
