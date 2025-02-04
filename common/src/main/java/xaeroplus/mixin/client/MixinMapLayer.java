package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xaero.map.region.LeveledRegionManager;
import xaero.map.region.MapLayer;
import xaeroplus.feature.extensions.OptimizedLeveledRegionManager;

@Mixin(value = MapLayer.class, remap = false)
public class MixinMapLayer {
    @Redirect(method = "<init>", at = @At(
        value = "NEW",
        target = "()Lxaero/map/region/LeveledRegionManager;"
    ))
    public LeveledRegionManager createOptimizedLeveledRegionManager() {
        return new OptimizedLeveledRegionManager();
    }
}
