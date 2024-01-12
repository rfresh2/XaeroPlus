package xaeroplus.mixin.client;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import xaero.common.minimap.waypoints.Waypoint;
import xaeroplus.feature.extensions.IWaypointDimension;

@Mixin(value = Waypoint.class, remap = false)
public class MixinWaypoint implements IWaypointDimension {

    private ResourceKey<Level> dimension = null;

    @Override
    public ResourceKey<Level> getDimension() {
        return dimension;
    }

    @Override
    public void setDimension(ResourceKey<Level> dimension) {
        this.dimension = dimension;
    }
}
