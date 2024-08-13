package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import xaero.common.gui.GuiWaypoints;
import xaero.common.minimap.waypoints.Waypoint;

import java.util.ArrayList;

@Mixin(value = GuiWaypoints.class, remap = false)
public interface AccessorGuiWaypoints {
    @Accessor(value = "waypointsSorted")
    ArrayList<Waypoint> getWaypointsSorted();
}
