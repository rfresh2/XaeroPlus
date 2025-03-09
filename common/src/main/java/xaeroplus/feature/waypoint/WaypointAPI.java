package xaeroplus.feature.waypoint;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.set.WaypointSet;
import xaero.hud.minimap.world.MinimapWorld;
import xaero.hud.path.XaeroPath;
import xaeroplus.mixin.client.AccessorWaypointSet;

import java.util.List;
import java.util.function.Consumer;

public class WaypointAPI {

    public static MinimapWorld getMinimapWorld(ResourceKey<Level> dim) {
        MinimapSession minimapSession = BuiltInHudModules.MINIMAP.getCurrentSession();
        if (minimapSession == null) return null;
        MinimapWorld currentWorld = minimapSession.getWorldManager().getCurrentWorld();
        if (currentWorld == null) return null;
        if (currentWorld.getDimId() == dim) {
            return currentWorld;
        }
        var rootContainer = minimapSession.getWorldManager().getCurrentRootContainer();
        for (MinimapWorld world : rootContainer.getWorlds()) {
            if (world.getDimId() == dim) {
                return world;
            }
        }
        String waystoneDimensionDirectoryName = minimapSession.getDimensionHelper().getDimensionDirectoryName(dim);
        String waystoneWpWorldNode = minimapSession.getWorldStateUpdater().getPotentialWorldNode(dim, true);
        XaeroPath waystoneWpContainerPath = minimapSession.getWorldState()
            .getAutoRootContainerPath()
            .resolve(waystoneDimensionDirectoryName)
            .resolve(waystoneWpWorldNode);
        return minimapSession.getWorldManager().getWorld(waystoneWpContainerPath);
    }

    public static WaypointSet getOrCreateWaypointSetInWorld(MinimapWorld minimapWorld, String setName) {
        WaypointSet waypointSet = minimapWorld.getWaypointSet(setName);
        if (waypointSet == null) {
            minimapWorld.addWaypointSet(setName);
            waypointSet = minimapWorld.getWaypointSet(setName);
        }
        return waypointSet;
    }

    public static void forEachWaypointSetInCurrentContainer(Consumer<List<Waypoint>> consumer) {
        MinimapSession minimapSession = BuiltInHudModules.MINIMAP.getCurrentSession();
        if (minimapSession == null) return;
        var rootContainer = minimapSession.getWorldManager().getCurrentRootContainer();
        var rootWorlds = rootContainer.getWorlds();
        for (var world : rootWorlds) {
            for (WaypointSet set : world.getIterableWaypointSets()) {
                consumer.accept(((AccessorWaypointSet) set).getList());
            }
        }
        for (var subContainer : rootContainer.getSubContainers()) {
            for (var world : subContainer.getWorlds()) {
                for (WaypointSet set : world.getIterableWaypointSets()) {
                    consumer.accept(((AccessorWaypointSet) set).getList());
                }
            }
        }
    }

    public static WaypointSet getCurrentWaypointSet() {
        MinimapSession minimapSession = BuiltInHudModules.MINIMAP.getCurrentSession();
        if (minimapSession == null) return null;
        MinimapWorld currentWorld = minimapSession.getWorldManager().getCurrentWorld();
        if (currentWorld == null) return null;
        return currentWorld.getCurrentWaypointSet();
    }
}
