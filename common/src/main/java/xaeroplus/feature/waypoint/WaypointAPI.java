package xaeroplus.feature.waypoint;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.set.WaypointSet;
import xaero.hud.minimap.world.MinimapWorld;
import xaero.hud.path.XaeroPath;
import xaeroplus.XaeroPlus;
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
        String dimensionDirectoryName = minimapSession.getDimensionHelper().getDimensionDirectoryName(dim);
        String worldNode = minimapSession.getWorldStateUpdater().getPotentialWorldNode(dim, true);
        XaeroPath containerPath = minimapSession.getWorldState()
            .getAutoRootContainerPath()
            .resolve(dimensionDirectoryName)
            .resolve(worldNode);
        return minimapSession.getWorldManager().getWorld(containerPath);
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

    public static void forEachMinimapWorld(Consumer<MinimapWorld> consumer) {
        MinimapSession minimapSession = BuiltInHudModules.MINIMAP.getCurrentSession();
        if (minimapSession == null) return;
        var rootContainer = minimapSession.getWorldManager().getCurrentRootContainer();
        for (MinimapWorld world : rootContainer.getWorlds()) {
            consumer.accept(world);
        }
        for (var subContainer : rootContainer.getSubContainers()) {
            for (MinimapWorld world : subContainer.getWorlds()) {
                consumer.accept(world);
            }
        }
    }

    public static void forEachWaypointSetInAllMinimapWorlds(Consumer<WaypointSet> consumer) {
        forEachMinimapWorld(world -> {
            for (WaypointSet set : world.getIterableWaypointSets()) {
                consumer.accept(set);
            }
        });
    }

    public static WaypointSet getCurrentWaypointSet() {
        MinimapSession minimapSession = BuiltInHudModules.MINIMAP.getCurrentSession();
        if (minimapSession == null) return null;
        MinimapWorld currentWorld = minimapSession.getWorldManager().getCurrentWorld();
        if (currentWorld == null) return null;
        return currentWorld.getCurrentWaypointSet();
    }

    public static void switchWaypointDimension(final ResourceKey<Level> dimension) {
        if (dimension == null) return;
        try {
            var minimapSession = BuiltInHudModules.MINIMAP.getCurrentSession();
            if (minimapSession == null) return;
            var worldManager = minimapSession.getWorldManager();
            if (worldManager == null) return;
            var currentWorld = worldManager.getCurrentWorld();
            if (currentWorld == null) return;
            if (currentWorld.getDimId() == dimension) return;
            var minimapWorld = getMinimapWorld(dimension);
            if (minimapWorld == null) return;
            if (minimapWorld.getFullPath() == null) return;
            var autoWorld = worldManager.getAutoWorld();
            if (autoWorld != null && autoWorld == minimapWorld) {
                minimapSession.getWorldState().setCustomWorldPath(null);
            } else {
                minimapSession.getWorldState().setCustomWorldPath(minimapWorld.getFullPath());
            }
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Failed switching waypoint dimension: {}", dimension, e);
        }
    }
}
