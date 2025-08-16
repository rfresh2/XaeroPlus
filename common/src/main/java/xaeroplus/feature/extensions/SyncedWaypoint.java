package xaeroplus.feature.extensions;

import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.hud.minimap.waypoint.WaypointPurpose;

public class SyncedWaypoint extends Waypoint {
    public SyncedWaypoint(
        int x, int y, int z,
        String name,
        String initials,
        WaypointColor color,
        WaypointPurpose purpose,
        boolean temp,
        boolean yIncluded
    ) {
        super(x, y, z, name, initials, color, purpose, temp, yIncluded);
    }

    public static SyncedWaypoint fromWaypoint(Waypoint waypoint) {
        return new SyncedWaypoint(
            waypoint.getX(),
            waypoint.getY(),
            waypoint.getZ(),
            waypoint.getName(),
            waypoint.getInitials(),
            waypoint.getWaypointColor(),
            waypoint.getPurpose(),
            waypoint.isTemporary(),
            waypoint.isYIncluded()
        );
    }

    public static SyncedWaypoint create(
        int x, int y, int z,
        String name,
        String initials,
        WaypointColor color
    ) {
        return new SyncedWaypoint(x, y, z, name, initials, color, WaypointPurpose.NORMAL, true, true);
    }

    public static SyncedWaypoint create(
        int x, int z,
        String name,
        String initials,
        WaypointColor color
    ) {
        return new SyncedWaypoint(x, 0, z, name, initials, color, WaypointPurpose.NORMAL, true, false);
    }
}
