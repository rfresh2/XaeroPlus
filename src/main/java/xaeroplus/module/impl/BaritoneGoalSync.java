package xaeroplus.module.impl;

import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalXZ;
import baritone.api.utils.interfaces.IGoalRenderPos;
import net.lenni0451.lambdaevents.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaero.common.XaeroMinimapSession;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.common.minimap.waypoints.WaypointSet;
import xaero.common.minimap.waypoints.WaypointWorld;
import xaero.common.minimap.waypoints.WaypointsManager;
import xaero.common.misc.OptimizedMath;
import xaero.map.mods.SupportMods;
import xaeroplus.Globals;
import xaeroplus.event.ClientTickEvent;
import xaeroplus.feature.extensions.IWaypointDimension;
import xaeroplus.module.Module;
import xaeroplus.util.BaritoneGoalHelper;
import xaeroplus.util.BaritoneHelper;
import xaeroplus.util.ChunkUtils;

import java.util.List;
import java.util.Optional;

import static net.minecraft.world.level.Level.NETHER;
import static net.minecraft.world.level.Level.OVERWORLD;

@Module.ModuleInfo()
public class BaritoneGoalSync extends Module {

    @EventHandler
    public void onClientTickEvent(final ClientTickEvent.Post event) {
        if (!BaritoneHelper.isBaritonePresent()) return;
        XaeroMinimapSession minimapSession = XaeroMinimapSession.getCurrentSession();
        if (minimapSession == null) return;
        final WaypointsManager waypointsManager = minimapSession.getWaypointsManager();
        WaypointSet waypointSet = waypointsManager.getWaypoints();
        if (waypointSet == null) return;
        WaypointWorld waypointWorld = waypointsManager.getCurrentWorld();
        if (waypointWorld == null) return;
        final List<Waypoint> waypoints = waypointSet.getList();
        Optional<Waypoint> baritoneGoalWaypoint = waypoints.stream()
                .filter(waypoint -> waypoint.getName().equals("Baritone Goal"))
                .findFirst();
        final Goal goal = BaritoneGoalHelper.getBaritoneGoal();
        if (goal == null) {
            baritoneGoalWaypoint.ifPresent(waypoint -> removeBaritoneGoalWaypoint(waypoints, waypoint));
            return;
        }
        final BlockPos baritoneGoalBlockPos = getBaritoneGoalBlockPos(goal);
        if (baritoneGoalBlockPos == null) {
            baritoneGoalWaypoint.ifPresent(waypoint -> removeBaritoneGoalWaypoint(waypoints, waypoint));
            return;
        };
        final double dimDiv = waypointsManager.getDimensionDivision(waypointWorld);
        final int x = OptimizedMath.myFloor(baritoneGoalBlockPos.getX() * dimDiv);
        final int z = OptimizedMath.myFloor(baritoneGoalBlockPos.getZ() * dimDiv);
        if (baritoneGoalWaypoint.isPresent()) {
            final Waypoint waypoint = baritoneGoalWaypoint.get();
            ResourceKey<Level> customDim = Globals.getCurrentDimensionId();
            ResourceKey<Level> actualDim = ChunkUtils.getActualDimension();
            double customDimDiv = 1.0;
            if (customDim != actualDim) {
                if (customDim == NETHER && actualDim == OVERWORLD) {
                    customDimDiv = 0.125;
                } else if (customDim == OVERWORLD && actualDim == NETHER) {
                    customDimDiv = 8.0;
                }
            }
            int expectedX = (int) (x * customDimDiv);
            int expectedZ = (int) (z * customDimDiv);
            if (waypoint.getX() != expectedX ||
                    waypoint.getY() != baritoneGoalBlockPos.getY() ||
                    waypoint.getZ() != expectedZ) {
                waypoint.setX(expectedX);
                waypoint.setY(baritoneGoalBlockPos.getY());
                waypoint.setZ(expectedZ);
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
            ((IWaypointDimension) waypoint).setDimension(waypointWorld.getDimId());
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
