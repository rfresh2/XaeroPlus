package xaeroplus.module.impl;

import com.google.common.hash.Hashing;
import net.lenni0451.lambdaevents.EventHandler;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.set.WaypointSet;
import xaero.hud.minimap.world.MinimapWorld;
import xaero.hud.minimap.world.MinimapWorldManager;
import xaero.hud.path.XaeroPath;
import xaeroplus.XaeroPlus;
import xaeroplus.event.ClientTickEvent;
import xaeroplus.event.XaeroWorldChangeEvent;
import xaeroplus.mixin.client.AccessorWaypointSet;
import xaeroplus.module.Module;
import xaeroplus.util.BlayWaystonesHelper;
import xaeroplus.util.ColorHelper.WaystoneColor;
import xaeroplus.util.FabricWaystonesHelper;
import xaeroplus.util.WaystonesHelper;

import java.util.List;
import java.util.Locale;

import static xaero.common.settings.ModSettings.COLORS;

public class WaystoneSync extends Module {
    private final BlayWaystonesHelper blayWaystonesHelper = new BlayWaystonesHelper();
    private WaystoneColor color = WaystoneColor.RANDOM;
    private boolean separateWaypointSet = false;
    private int visibilityType = 0;

    @Override
    public void onEnable() {
        if (WaystonesHelper.isWaystonesPresent()) {
            blayWaystonesHelper.subscribeWaystonesEvent();
        }
        if (WaystonesHelper.isFabricWaystonesPresent()) {
            FabricWaystonesHelper.subcribeWaystonesEventsRunnable.run();
        }
        reloadWaystones();
    }

    @Override
    public void onDisable() {
        blayWaystonesHelper.currentWaystoneTypeMap.clear();
    }

    @EventHandler
    public void onXaeroWorldChangeEvent(final XaeroWorldChangeEvent event) {
        if (event.worldId() == null) {
            blayWaystonesHelper.currentWaystoneTypeMap.clear();
        }
    }

    @EventHandler
    public void onClientTickEvent(final ClientTickEvent.Post event) {
        if (WaystonesHelper.isWaystonesPresent()) {
            synchronized (blayWaystonesHelper.lock) {
                if (blayWaystonesHelper.shouldSync) {
                    if (syncBlayWaystones()) {
                        blayWaystonesHelper.shouldSync = false;
                    }
                }
            }
        } else if (WaystonesHelper.isFabricWaystonesPresent()) {
            if (FabricWaystonesHelper.shouldSync) {
                syncFabricWaystones();
                FabricWaystonesHelper.shouldSync = false;
            }
        }
    }

    public void syncFabricWaystones() {
        commonWaystoneSync(FabricWaystonesHelper.getWaystones());
    }

    public boolean syncBlayWaystones() {
        return commonWaystoneSync(blayWaystonesHelper.getCurrentWaystones());
    }

    public boolean commonWaystoneSync(final List<Waystone> waystones) {
        try {
            MinimapSession minimapSession = BuiltInHudModules.MINIMAP.getCurrentSession();
            if (minimapSession == null) return false;
            MinimapWorldManager worldManager = minimapSession.getWorldManager();
            if (worldManager == null) return false;
            MinimapWorld currentWorld = worldManager.getCurrentWorld();
            if (currentWorld == null) return false;
            clearWaystoneWaypoints(minimapSession);
            for (Waystone waystone : waystones) {
                try {
                    waypointsListSync(waystone, getWaypointSet(waystone, minimapSession));
                } catch (final Exception e) {
                    XaeroPlus.LOGGER.error("Error syncing waystone: {}", waystone.name(), e);
                }
            }
            return true;
        } catch (Exception e) {
            XaeroPlus.LOGGER.error("Error syncing waystones", e);
            return true; // stops immediate retry. we'll still spam logs on the next iteration though
        }
    }

    // iterate over ALL waypoint sets and lists and remove waystones
    private void clearWaystoneWaypoints(final MinimapSession minimapSession) {
        var rootContainer = minimapSession.getWorldManager().getCurrentRootContainer();
        var rootWorlds = rootContainer.getWorlds();
        for (var world : rootWorlds) {
            for (WaypointSet set : world.getIterableWaypointSets()) {
                ((AccessorWaypointSet) set).getList().removeIf(WaystoneSync::isWaystoneWaypoint);
            }
        }
        for (var subContainer : rootContainer.getSubContainers()) {
            for (var world : subContainer.getWorlds()) {
                for (WaypointSet set : world.getIterableWaypointSets()) {
                    ((AccessorWaypointSet) set).getList().removeIf(WaystoneSync::isWaystoneWaypoint);
                }
            }
        }
    }

    private static boolean isWaystoneWaypoint(Waypoint waypoint) {
        return waypoint.isTemporary() && waypoint.getName().endsWith(" [Waystone]");
    }

    private void waypointsListSync(final Waystone waystone, final WaypointSet waypointsList) {
        Waypoint waystoneWp = new Waypoint(
            waystone.x(),
            waystone.y(),
            waystone.z(),
            waystone.name() + " [Waystone]",
            waystone.name().isEmpty()
                ? "W"
                : waystone.name().substring(0, 1).toUpperCase(Locale.ROOT),
            getWaystoneColor(waystone),
            0,
            true
        );
        waystoneWp.setVisibilityType(visibilityType);
        waypointsList.add(waystoneWp);
    }

    private WaypointSet getWaypointSet(final Waystone waystone, final MinimapSession minimapSession) {
        final String waypointSetName = this.separateWaypointSet ? "Waystones" : "gui.xaero_default";
        final MinimapWorld waypointWorld = getWaypointWorldForWaystone(waystone, minimapSession);
        WaypointSet waypointSet = waypointWorld.getWaypointSet(waypointSetName);
        if (waypointSet == null) {
            waypointSet = WaypointSet.Builder.begin().setName(waypointSetName).build();
            waypointWorld.addWaypointSet(waypointSet);
        }
        return waypointSet;
    }

    private MinimapWorld getWaypointWorldForWaystone(final Waystone waystone,
                                                      MinimapSession minimapSession) {
        ResourceKey<Level> waystoneDimension = waystone.dimension();
        String waystoneDimensionDirectoryName = minimapSession.getDimensionHelper().getDimensionDirectoryName(waystoneDimension);
        MinimapWorld currentWpWorld = minimapSession.getWorldManager().getCurrentWorld();
        if (currentWpWorld == null) {
            throw new RuntimeException("WaystoneSync: current waypoint world is null");
        }
        if (currentWpWorld.getDimId() == waystoneDimension) {
            return currentWpWorld;
        }
        String waystoneWpWorldNode = minimapSession.getWorldStateUpdater().getPotentialWorldNode(waystoneDimension, true, minimapSession);
        XaeroPath waystoneWpContainerPath = minimapSession.getWorldState()
            .getAutoRootContainerPath()
            .resolve(waystoneDimensionDirectoryName)
            .resolve(waystoneWpWorldNode);
        return minimapSession.getWorldManager().getWorld(waystoneWpContainerPath);
    }

    private int getWaystoneColor(Waystone waystone) {
        if (color == WaystoneColor.RANDOM) {
            return Math.abs(Hashing.murmur3_128().hashUnencodedChars(waystone.name()).asInt()) % COLORS.length;
        } else {
            return color.getColorIndex();
        }
    }

    public void setColor(final WaystoneColor color) {
        this.color = color;
        reloadWaystones();
    }

    public void setWaypointSet(final boolean waypointSet) {
        this.separateWaypointSet = waypointSet;
        reloadWaystones();
    }

    public void setVisibilityType(final int visibilityType) {
        this.visibilityType = visibilityType;
        reloadWaystones();
    }

    public void reloadWaystones() {
        blayWaystonesHelper.shouldSync = true;
        FabricWaystonesHelper.shouldSync = true;
    }

    public record Waystone(String name, ResourceKey<Level> dimension, int x, int y, int z) { }
}
