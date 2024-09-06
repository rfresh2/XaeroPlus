package xaeroplus.module.impl;

import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalXZ;
import baritone.api.utils.interfaces.IGoalRenderPos;
import net.lenni0451.lambdaevents.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.common.misc.OptimizedMath;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.set.WaypointSet;
import xaero.hud.minimap.world.MinimapWorld;
import xaero.map.mods.SupportMods;
import xaeroplus.XaeroPlus;
import xaeroplus.event.ClientTickEvent;
import xaeroplus.module.Module;
import xaeroplus.util.BaritoneGoalHelper;
import xaeroplus.util.BaritoneHelper;
import xaeroplus.util.ChunkUtils;

import java.lang.ref.WeakReference;

import static net.minecraft.world.level.Level.NETHER;
import static net.minecraft.world.level.Level.OVERWORLD;

public class BaritoneGoalSync extends Module {
    private static final WeakReference nullRef = new WeakReference<>(null);
    private WeakReference<Waypoint> baritoneWpRef = nullRef;
    private WeakReference<WaypointSet> baritoneWpSetRef = nullRef;
    private WeakReference<MinimapWorld> baritoneWpMinimapWorldRef = nullRef;
    private WeakReference<BlockPos> baritoneGoalPos = nullRef;
    private ResourceKey<Level> baritoneWpDimension = OVERWORLD;

    @EventHandler
    public void onClientTickEvent(final ClientTickEvent.Post event) {
        if (!BaritoneHelper.isBaritonePresent()) return;
        MinimapSession minimapSession = BuiltInHudModules.MINIMAP.getCurrentSession();
        if (minimapSession == null) return;
        MinimapWorld currentWorld = minimapSession.getWorldManager().getCurrentWorld();
        if (currentWorld == null) return;
        WaypointSet currentWpSet = currentWorld.getCurrentWaypointSet();
        if (currentWpSet == null) return;
        try {
            final Goal goal = BaritoneGoalHelper.getBaritoneGoal();
            if (goal == null) {
                clearBaritoneWpAndState();
                return;
            }
            final BlockPos baritoneGoalBlockPos = getBaritoneGoalBlockPos(goal);
            if (baritoneGoalBlockPos == null) {
                clearBaritoneWpAndState();
                return;
            }

            if (baritoneGoalPos.get() == null) {
                baritoneGoalPos = new WeakReference<>(baritoneGoalBlockPos);
            }

            if (baritoneGoalPos.get() != null && !baritoneGoalPos.get().equals(baritoneGoalBlockPos)) {
                clearBaritoneWpAndState();
                baritoneGoalPos = new WeakReference<>(baritoneGoalBlockPos);
            }

            if (baritoneWpMinimapWorldRef.get() == null || currentWorld != baritoneWpMinimapWorldRef.get()
                || baritoneWpSetRef.get() == null || baritoneWpSetRef.get() != currentWpSet) {
                if (baritoneWpSetRef.get() != null && baritoneWpRef.get() != null) {
                    baritoneWpSetRef.get().remove(baritoneWpRef.get());
                }
                baritoneWpRef = nullRef;
                initBaritoneWpWorld(currentWorld);
            }

            double customDimDiv = getBaritoneWpDimDiv();
            final int x = (int) (OptimizedMath.myFloor(baritoneGoalBlockPos.getX()) * customDimDiv);
            final int z = (int) (OptimizedMath.myFloor(baritoneGoalBlockPos.getZ()) * customDimDiv);
            Waypoint baritoneWp = baritoneWpRef.get();
            if (baritoneWp != null) {
                if (baritoneWp.getX() != x || baritoneWp.getZ() != z) {
                    baritoneWp.setX(x);
                    baritoneWp.setY(baritoneGoalBlockPos.getY());
                    baritoneWp.setZ(z);
                    SupportMods.xaeroMinimap.requestWaypointsRefresh();
                }
            } else {
                baritoneWp = new Waypoint(
                    x,
                    baritoneGoalBlockPos.getY(),
                    z,
                    "Baritone Goal",
                    "B",
                    10, // green
                    0,
                    true);
                baritoneWpRef = new WeakReference<>(baritoneWp);
                baritoneWpSetRef.get().add(baritoneWp);
                SupportMods.xaeroMinimap.requestWaypointsRefresh();
            }
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error in Baritone goal sync", e);
        }
    }

    private double getBaritoneWpDimDiv() {
        if (baritoneWpMinimapWorldRef.get() == null) return 1.0;
        ResourceKey<Level> baritoneWpMinimapWorldDimId = baritoneWpMinimapWorldRef.get().getDimId();
        double customDimDiv = 1.0;
        if (baritoneWpMinimapWorldDimId != baritoneWpDimension) {
            if (baritoneWpMinimapWorldDimId == NETHER && baritoneWpDimension == OVERWORLD) {
                customDimDiv = 0.125;
            } else if (baritoneWpMinimapWorldDimId == OVERWORLD && baritoneWpDimension == NETHER) {
                customDimDiv = 8.0;
            }
        }
        return customDimDiv;
    }

    private void initBaritoneWpWorld(final MinimapWorld currentWorld) {
        baritoneWpMinimapWorldRef = new WeakReference<>(currentWorld);
        WaypointSet waypointSet = baritoneWpMinimapWorldRef.get().getCurrentWaypointSet();
        baritoneWpSetRef = new WeakReference<>(waypointSet);
    }

    private void clearBaritoneWpAndState() {
        if (baritoneWpRef.get() != null) baritoneWpSetRef.get().remove(this.baritoneWpRef.get());
        baritoneWpRef = nullRef;
        baritoneWpSetRef = nullRef;
        baritoneWpMinimapWorldRef = nullRef;
        baritoneGoalPos = nullRef;
        baritoneWpDimension = ChunkUtils.getActualDimension();
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
