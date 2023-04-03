package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xaero.map.MapProcessor;
import xaero.map.file.worldsave.WorldDataReader;
import xaeroplus.XaeroPlus;

@Mixin(value = WorldDataReader.class, remap = false)
public class MixinWorldDataReader {
    @Redirect(method = "buildTileChunk", at = @At(value = "INVOKE", target = "Lxaero/map/MapProcessor;getCurrentDimension()Ljava/lang/String;"))
    public String getCustomDimension(final MapProcessor instance) {
        return instance.getDimensionName(XaeroPlus.customDimensionId);
    }
}
