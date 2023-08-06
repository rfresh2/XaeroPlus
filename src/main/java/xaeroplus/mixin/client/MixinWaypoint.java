package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import xaero.common.minimap.waypoints.Waypoint;
import xaeroplus.util.IWaypointDimension;

@Mixin(value = Waypoint.class, remap = false)
public class MixinWaypoint implements IWaypointDimension {

    private int dimension = Integer.MIN_VALUE;

    @Override
    public int getDimension() {
        return dimension;
    }

    @Override
    public void setDimension(int dimension) {
        this.dimension = dimension;
    }
}
