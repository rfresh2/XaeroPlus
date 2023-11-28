package xaeroplus.mixin.client;

import net.minecraft.block.BlockObsidian;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.MapProcessor;
import xaero.map.MapWriter;
import xaero.map.biome.BiomeColorCalculator;
import xaero.map.cache.BlockStateShortShapeCache;
import xaero.map.misc.Misc;
import xaero.map.region.*;
import xaero.map.world.MapDimension;
import xaeroplus.settings.XaeroPlusSettingRegistry;

import java.util.ArrayList;
@Mixin(value = MapPixel.class, remap = false)
public abstract class MixinMapPixel {

    @Shadow
    protected int state = 0;

    @Inject(method = "getPixelColours", at = @At("RETURN"))
    public void getPixelColours(
            int[] result_dest,
            MapWriter mapWriter,
            World world,
            MapDimension dim,
            MapTileChunk tileChunk,
            MapTileChunk prevChunk,
            MapTileChunk prevChunkDiagonal,
            MapTileChunk prevChunkHorisontal,
            MapTile mapTile,
            int x,
            int z,
            MapBlock block,
            int height,
            int topHeight,
            int caveStart,
            int caveDepth,
            ArrayList<Overlay> overlays,
            BlockPos.MutableBlockPos mutableGlobalPos,
            float shadowR,
            float shadowG,
            float shadowB,
            BiomeColorCalculator biomeColorCalculator,
            MapProcessor mapProcessor,
            OverlayManager overlayManager,
            BlockStateShortShapeCache blockStateShortShapeCache,
            CallbackInfo ci
    ) {
        if (XaeroPlusSettingRegistry.transparentObsidianRoofSetting.getValue()) {
            int state = this.state;
            IBlockState blockState = Misc.getStateById(state);
            boolean isObsidian = blockState.getBlock() instanceof BlockObsidian;
            if (isObsidian) {
                result_dest[3] = (int) XaeroPlusSettingRegistry.transparentObsidianRoofDarkeningSetting.getValue();
            }
        }
    }
}
