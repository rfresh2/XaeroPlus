package xaeroplus.mixin.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
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
            final Level world,
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
            final BlockPos.MutableBlockPos mutableGlobalPos,
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
            if (state.getBlock() == Blocks.OBSIDIAN) {
                result_dest[3] = (int) XaeroPlusSettingRegistry.transparentObsidianRoofDarkeningSetting.getValue();
            } else if (state.getBlock() == Blocks.SNOW) {
                result_dest[3] = (int) XaeroPlusSettingRegistry.transparentObsidianRoofSnowOpacitySetting.getValue();
            }
        }
    }
}
