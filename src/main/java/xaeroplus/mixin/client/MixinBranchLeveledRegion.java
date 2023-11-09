package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xaero.map.MapProcessor;
import xaero.map.region.BranchLeveledRegion;
import xaero.map.region.LeveledRegion;
import xaero.map.region.MapRegion;
import xaero.map.region.texture.BranchRegionTexture;
import xaero.map.world.MapDimension;
import xaero.map.world.MapWorld;
import xaeroplus.feature.extensions.CustomDimensionMapProcessor;

@Mixin(value = BranchLeveledRegion.class, remap = false)
public abstract class MixinBranchLeveledRegion extends LeveledRegion<BranchRegionTexture> {

    public MixinBranchLeveledRegion(final String worldId, final String dimId, final String mwId, final MapDimension dim, final int level, final int leveledX, final int leveledZ, final int caveLayer, final BranchLeveledRegion parent) {
        super(worldId, dimId, mwId, dim, level, leveledX, leveledZ, caveLayer, parent);
    }

    @Redirect(method = "checkAndTrackRegionExistence", at = @At(value = "INVOKE", target = "Lxaero/map/world/MapWorld;getCurrentDimension()Lxaero/map/world/MapDimension;"))
    public MapDimension getCustomDimension(final MapWorld instance) {
        return dim;
    }

    @Redirect(method = "checkForUpdates", at = @At(value = "INVOKE", target = "Lxaero/map/MapProcessor;getMapRegion(IIIZ)Lxaero/map/region/MapRegion;"))
    public MapRegion redirectGetMapRegion(MapProcessor mapProcessor, int caveLayer, int regX, int regZ, boolean create) {
        return ((CustomDimensionMapProcessor) mapProcessor).getMapRegionCustomDimension(caveLayer, regX, regZ, create, dim.getDimId());
    }

}
