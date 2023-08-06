package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import xaero.map.mods.gui.Waypoint;
import xaeroplus.util.IWaypointDimension;

@Mixin(value = Waypoint.class, remap = false)
public class MixinGuiWaypoint implements IWaypointDimension {
    @Shadow
    private Object original;

    @Override
    public int getDimension() {
        if (original instanceof xaero.common.minimap.waypoints.Waypoint) {
            return ((IWaypointDimension) original).getDimension();
        }
        return 0;
    }

    @Override
    public void setDimension(final int dimension) {
        if (original instanceof xaero.common.minimap.waypoints.Waypoint) {
            ((IWaypointDimension) original).setDimension(dimension);
        }
    }
}
