package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xaero.map.highlight.RegionHighlightExistenceTracker;
import xaero.map.region.LayeredRegionManager;
import xaero.map.region.MapLayer;
import xaero.map.world.MapDimension;
import xaeroplus.feature.extensions.OptimizedMapLayer;

@Mixin(value = LayeredRegionManager.class, remap = false)
public class MixinLayeredRegionManager {
    @Redirect(method = "getLayer", at = @At(
        value = "NEW",
        target = "(Lxaero/map/world/MapDimension;Lxaero/map/highlight/RegionHighlightExistenceTracker;)Lxaero/map/region/MapLayer;"
    ))
    public MapLayer createOptimizedMapLayer(final MapDimension mapDimension, final RegionHighlightExistenceTracker regionHighlightExistenceTracker) {
        return new OptimizedMapLayer(mapDimension, regionHighlightExistenceTracker);
    }
}
