package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xaero.map.MapLimiter;
import xaero.map.world.MapDimension;
import xaero.map.world.MapWorld;
import xaeroplus.util.Shared;

@Mixin(value = MapLimiter.class, remap = false)
public class MixinMapLimiter {

    @Redirect(method = "applyLimit", at = @At(value = "INVOKE", target = "Lxaero/map/world/MapWorld;getCurrentDimension()Lxaero/map/world/MapDimension;"))
    public MapDimension getCustomDimension(final MapWorld instance) {
        return instance.getDimension(Shared.customDimensionId);
    }
}
