package xaeroplus.module.impl;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalXZ;
import baritone.api.utils.interfaces.IGoalRenderPos;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.common.misc.OptimizedMath;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.set.WaypointSet;
import xaero.hud.minimap.world.MinimapWorld;
import xaero.map.mods.SupportMods;
import xaeroplus.module.Module;
import xaeroplus.util.BaritoneHelper;

import java.util.List;

@Module.ModuleInfo()
public class BaritoneGoalSync extends Module {

    @SubscribeEvent
    public void onClientTickEvent(final TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!BaritoneHelper.isBaritonePresent()) return;
        MinimapSession minimapSession = BuiltInHudModules.MINIMAP.getCurrentSession();
        if (minimapSession == null) return;
        MinimapWorld currentWorld = minimapSession.getWorldManager().getCurrentWorld();
        if (currentWorld == null) return;
        WaypointSet currentWaypointSet = currentWorld.getCurrentWaypointSet();
        Waypoint currentBaritoneWp = null;
        for (Waypoint wp : currentWaypointSet.getWaypoints()) {
            if (wp.getName().equals("Baritone Goal")) {
                currentBaritoneWp = wp;
                break;
            }
        }
        boolean baritoneWpExists = currentBaritoneWp != null;
        final Goal goal = BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().getGoal();
        if (goal == null) {
            if (baritoneWpExists) currentWaypointSet.remove(currentBaritoneWp);
            return;
        }
        final BlockPos baritoneGoalBlockPos = getBaritoneGoalBlockPos(goal);
        if (baritoneGoalBlockPos == null) {
            if (baritoneWpExists) currentWaypointSet.remove(currentBaritoneWp);
            return;
        }
        // todo: this no longer has any idea about the baritone goal's dimension
        //      so the dim div will be completely off if the player has a wp set of nether open in the ow and visa versa
        final int x = OptimizedMath.myFloor(baritoneGoalBlockPos.getX());
        final int z = OptimizedMath.myFloor(baritoneGoalBlockPos.getZ());
        if (baritoneWpExists) {
            currentBaritoneWp.setX(x);
            currentBaritoneWp.setY(baritoneGoalBlockPos.getY());
            currentBaritoneWp.setZ(z);
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
            currentWaypointSet.add(waypoint);
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
