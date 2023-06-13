package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import xaero.common.gui.GuiWaypoints;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.common.minimap.waypoints.WaypointWorld;
import xaero.common.minimap.waypoints.WaypointsManager;

import java.util.ArrayList;

@Mixin(value = GuiWaypoints.class, remap = false)
public interface MixinGuiWaypointsAccessor {
    @Accessor(value = "waypointsSorted")
    ArrayList<Waypoint> getWaypointsSorted();

    @Accessor(value = "waypointsManager")
    WaypointsManager getWaypointsManager();

    @Accessor(value = "displayedWorld")
    WaypointWorld getDisplayedWorld();
}
