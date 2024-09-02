package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.waypoint.set.WaypointSet;

import java.util.List;

@Mixin(value = WaypointSet.class, remap = false)
public interface AccessorWaypointSet {
    // we could alternatively just cast the iterable getter back to list
    // but doing an accessor should cause compile errors if the type ever changes from under us in future updates
    @Accessor
    List<Waypoint> getList();
}
