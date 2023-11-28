package xaeroplus.mixin.client;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.MapProcessor;
import xaero.map.MapWriter;
import xaero.map.biome.BlockTintProvider;
import xaero.map.cache.BlockStateShortShapeCache;
import xaero.map.region.*;
import xaero.map.world.MapDimension;
import xaeroplus.settings.XaeroPlusSettingRegistry;

import java.util.ArrayList;
@Mixin(value = MapPixel.class, remap = false)
public abstract class MixinMapPixel {

    @Shadow
    protected BlockState state;

    @Inject(method = "getPixelColours", at = @At("RETURN"), remap = true)
    public void getPixelColours(
            final int[] result_dest,
            final MapWriter mapWriter,
            final World world,
            MapDimension dim,
            final Registry<Block> blockRegistry,
            final MapTileChunk tileChunk,
            final MapTileChunk prevChunk,
            final MapTileChunk prevChunkDiagonal,
            final MapTileChunk prevChunkHorisontal,
            final MapTile mapTile,
            final int x,
            final int z,
            final MapBlock block,
            final int height,
            final int topHeight,
            final int caveStart,
            final int caveDepth,
            final ArrayList<Overlay> overlays,
            final BlockPos.Mutable mutableGlobalPos,
            final Registry<Biome> biomeRegistry,
            Registry<DimensionType> dimensionTypes,
            final float shadowR,
            final float shadowG,
            final float shadowB,
            final BlockTintProvider blockTintProvider,
            final MapProcessor mapProcessor,
            final OverlayManager overlayManager,
            final BlockStateShortShapeCache blockStateShortShapeCache,
            final CallbackInfo ci
    ) {
        if (XaeroPlusSettingRegistry.transparentObsidianRoofSetting.getValue()) {
            boolean isObsidian = state.getBlock() == Blocks.OBSIDIAN;
            if (isObsidian) {
                result_dest[3] = (int) XaeroPlusSettingRegistry.transparentObsidianRoofDarkeningSetting.getValue();
            }
        }
    }
}
