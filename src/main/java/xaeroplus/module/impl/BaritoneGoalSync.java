package xaeroplus.module.impl;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalXZ;
import baritone.api.utils.interfaces.IGoalRenderPos;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import xaero.common.XaeroMinimapSession;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.common.minimap.waypoints.WaypointSet;
import xaero.common.minimap.waypoints.WaypointsManager;
import xaero.common.misc.OptimizedMath;
import xaero.map.mods.SupportMods;
import xaeroplus.module.Module;
import xaeroplus.util.BaritoneHelper;

import java.util.List;
import java.util.Optional;

@Module.ModuleInfo()
public class BaritoneGoalSync extends Module {

    @SubscribeEvent
    public void onClientTickEvent(final TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!BaritoneHelper.isBaritonePresent()) return;
        XaeroMinimapSession minimapSession = XaeroMinimapSession.getCurrentSession();
        if (minimapSession == null) return;
        final WaypointsManager waypointsManager = minimapSession.getWaypointsManager();
        WaypointSet waypointSet = waypointsManager.getWaypoints();
        if (waypointSet == null) return;
        final List<Waypoint> waypoints = waypointSet.getList();
        Optional<Waypoint> baritoneGoalWaypoint = waypoints.stream()
                .filter(waypoint -> waypoint.getName().equals("Baritone Goal"))
                .findFirst();
        final Goal goal = BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().getGoal();
        if (goal == null) {
            baritoneGoalWaypoint.ifPresent(waypoint -> removeBaritoneGoalWaypoint(waypoints, waypoint));
            return;
        }
        final BlockPos baritoneGoalBlockPos = getBaritoneGoalBlockPos(goal);
        if (baritoneGoalBlockPos == null) {
            baritoneGoalWaypoint.ifPresent(waypoint -> removeBaritoneGoalWaypoint(waypoints, waypoint));
            return;
        };
        final double dimDiv = waypointsManager.getDimensionDivision(waypointsManager.getCurrentContainerID());
        final int x = OptimizedMath.myFloor(baritoneGoalBlockPos.getX() * dimDiv);
        final int z = OptimizedMath.myFloor(baritoneGoalBlockPos.getZ() * dimDiv);
        if (baritoneGoalWaypoint.isPresent()) {
            final Waypoint waypoint = baritoneGoalWaypoint.get();
            if (waypoint.getX() != x ||
                    waypoint.getY() != baritoneGoalBlockPos.getY() ||
                    waypoint.getZ() != z) {
                waypoint.setX(x);
                waypoint.setY(baritoneGoalBlockPos.getY());
                waypoint.setZ(z);
                SupportMods.xaeroMinimap.requestWaypointsRefresh();
            }
        } else {
            final Waypoint waypoint = new Waypoint(
                    x,
                    baritoneGoalBlockPos.getY(),
                    z,
                    "Baritone Goal",
                    "B",
                    10, // green
                    0,
                    true);
            waypoints.add(waypoint);
            SupportMods.xaeroMinimap.requestWaypointsRefresh();
        }

    }

    private void removeBaritoneGoalWaypoint(List<Waypoint> waypoints, Waypoint waypoint) {
        if (waypoints.remove(waypoint)) {
            SupportMods.xaeroMinimap.requestWaypointsRefresh();
        }
    }

    private BlockPos getBaritoneGoalBlockPos(Goal goal) {
        if (goal instanceof GoalXZ) {
            return new BlockPos(((GoalXZ) goal).getX(), 64, ((GoalXZ) goal).getZ());
        } else if (goal instanceof IGoalRenderPos) {
            return ((IGoalRenderPos) goal).getGoalPos();
        } else {
            return null;
        }
    }
}
