package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xaero.map.highlight.RegionHighlightExistenceTracker;
import xaero.map.region.LayeredRegionManager;
import xaero.map.region.MapLayer;
import xaero.map.world.MapDimension;
import xaeroplus.feature.extensions.OptimizedMapLayer;
import xaeroplus.settings.Settings;

@Mixin(value = LayeredRegionManager.class, remap = false)
public class MixinLayeredRegionManager {
    @WrapOperation(method = "getLayer", at = @At(
        value = "NEW",
        target = "(Lxaero/map/world/MapDimension;Lxaero/map/highlight/RegionHighlightExistenceTracker;)Lxaero/map/region/MapLayer;"
    ))
    public MapLayer createOptimizedMapLayer(final MapDimension mapDimension, final RegionHighlightExistenceTracker regionHighlightExistenceTracker, final Operation<MapLayer> original) {
        if (Settings.REGISTRY.optimizeRegionDetectionLookups.get()) {
            return new OptimizedMapLayer(mapDimension, regionHighlightExistenceTracker);
        } else {
            return original.call(mapDimension, regionHighlightExistenceTracker);
        }
    }
}
