package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xaero.map.region.BranchLeveledRegion;
import xaero.map.world.MapDimension;
import xaero.map.world.MapWorld;
import xaeroplus.XaeroPlus;

@Mixin(value = BranchLeveledRegion.class, remap = false)
public class MixinBranchLeveledRegion {

    @Redirect(method = "checkAndTrackRegionExistence", at = @At(value = "INVOKE", target = "Lxaero/map/world/MapWorld;getCurrentDimension()Lxaero/map/world/MapDimension;"))
    public MapDimension getCustomDimension(final MapWorld instance) {
        return instance.getDimension(XaeroPlus.customDimensionId);
    }
}
