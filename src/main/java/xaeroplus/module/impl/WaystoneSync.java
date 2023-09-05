package xaeroplus.module.impl;

import com.collarmc.pounce.Subscribe;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.waystones.api.IWaystone;
import net.blay09.mods.waystones.api.KnownWaystonesEvent;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import wraith.fwaystones.FabricWaystones;
import wraith.fwaystones.access.WaystoneValue;
import xaero.common.XaeroMinimapSession;
import xaero.common.minimap.waypoints.*;
import xaero.common.misc.OptimizedMath;
import xaero.map.mods.SupportMods;
import xaeroplus.XaeroPlus;
import xaeroplus.event.ClientTickEvent;
import xaeroplus.event.XaeroWorldChangeEvent;
import xaeroplus.module.Module;
import xaeroplus.util.IWaypointDimension;
import xaeroplus.util.WaypointsHelper;
import xaeroplus.util.WaystonesHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import static net.minecraft.world.World.NETHER;
import static xaero.common.settings.ModSettings.COLORS;

@Module.ModuleInfo()
public class WaystoneSync extends Module {
    private boolean subscribed = false;
    private boolean shouldSync = false;
    private List<IWaystone> toSyncWaystones = new ArrayList<>();
    int fwaystonesTickC = 0;

    @Override
    public void onEnable() {
        if (WaystonesHelper.isWaystonesPresent()) {
            if (!subscribed) {
                subscribed = true;
                subscribeWaystonesEvent();
            }
        }
    }

    private void subscribeWaystonesEvent() {
        Balm.getEvents().onEvent(KnownWaystonesEvent.class, this::onKnownWaystonesEvent);
    }

    @Override
    public void onDisable() {
        toSyncWaystones = new ArrayList<>();
    }

    @Subscribe
    public void onXaeroWorldChangeEvent(final XaeroWorldChangeEvent event) {
        if (event.worldId() == null) {
            toSyncWaystones = new ArrayList<>();
        }
    }

    private void onKnownWaystonesEvent(final KnownWaystonesEvent event) {
        toSyncWaystones = event.getWaystones();
        shouldSync = true;
    }

    @Subscribe
    public void onClientTickEvent(final ClientTickEvent.Post event) {
        if (WaystonesHelper.isWaystonesPresent()) {
            if (shouldSync) {
                if (syncMainWaystones()) {
                    shouldSync = false;
                    toSyncWaystones = new ArrayList<>();
                }
            }
        } else if (WaystonesHelper.isFabricWaystonesPresent()) {
            syncFabricWaystones();
        }
    }

    public void syncFabricWaystones() {
        if (fwaystonesTickC++ % 20 != 0) return;
        if (fwaystonesTickC > 100) fwaystonesTickC = 0;
        ConcurrentHashMap<String, WaystoneValue> waystones = FabricWaystones.WAYSTONE_STORAGE.WAYSTONES;
        if (waystones == null) return;
        commonWaystoneSync(waystones.values().stream()
                               .map(waystone -> new Waystone(waystone.getWaystoneName(),
                                                             RegistryKey.of(RegistryKeys.WORLD, new Identifier(waystone.getWorldName())),
                                                             waystone.way_getPos().getX(),
                                                             waystone.way_getPos().getY() + 1, // avoid teleporting directly into the waystone
                                                             waystone.way_getPos().getZ())).toList());
    }

    public boolean syncMainWaystones() {
        return commonWaystoneSync(toSyncWaystones.stream()
                                      .map(waystone -> new Waystone(waystone.getName(),
                                                                    waystone.getDimension(),
                                                                    waystone.getPos().getX(),
                                                                    waystone.getPos().getY() + 1, // avoid teleporting directly into the waystone
                                                                    waystone.getPos().getZ())).toList());
    }

    public boolean commonWaystoneSync(final List<Waystone> waystones) {
        XaeroMinimapSession minimapSession = XaeroMinimapSession.getCurrentSession();
        if (minimapSession == null) return false;
        final WaypointsManager waypointsManager = minimapSession.getWaypointsManager();
        WaypointSet waypointSet = waypointsManager.getWaypoints();
        if (waypointSet == null) return false;
        final List<Waypoint> waypoints = waypointSet.getList();
        waypoints.removeIf(waypoint -> waypoint.isTemporary() && waypoint.getName().endsWith(" [Waystone]"));
        final String currentContainerId = waypointsManager.getCurrentContainerID();
        final RegistryKey<World> currentWaypointDimension = WaypointsHelper.getDimensionKeyForWaypointWorldKey(currentContainerId);
        for (Waystone waystoneEntry : waystones) {
//            XaeroPlus.LOGGER.info("Syncing waystone entry: {} pos: {}, {}, {}", waystoneEntry.name(), waystoneEntry.x(), waystoneEntry.y(), waystoneEntry.z());
            RegistryKey<World> waystoneDimension = waystoneEntry.dimension();
            if (waystoneDimension.getValue() != currentWaypointDimension.getValue()
                && isVanillaDimension(waystoneDimension // this will most likely fail, just skip it and sync to current set
            )) {
                crossDimWaystoneSync(waystoneEntry, waypointsManager, currentContainerId);
            } else {
                currentDimWaystoneSync(waystoneEntry, waypointsManager, waypoints);
            }
        }
        SupportMods.xaeroMinimap.requestWaypointsRefresh();
        return true;
    }

    private boolean isVanillaDimension(RegistryKey<World> dimension) {
        return dimension == World.OVERWORLD || dimension == World.NETHER || dimension == World.END;
    }

    private void crossDimWaystoneSync(Waystone waystone, WaypointsManager waypointsManager, String currentContainerId) {
        try {
            final RegistryKey<World> waystoneDimension = waystone.dimension();
            final String waystoneDimensionDirectoryName = waypointsManager.getDimensionDirectoryName(waystoneDimension);
            final int waystoneDim = WaypointsHelper.getDimensionForWaypointWorldKey(waystoneDimensionDirectoryName);
            WaypointWorldContainer waypointWorldContainer = waypointsManager.getWorldContainer(currentContainerId.substring(
                0,
                currentContainerId.lastIndexOf(37) + 1) + waystoneDim);
            WaypointWorld crossDimWaypointWorld = waypointWorldContainer.worlds.get("waypoints");
            if (crossDimWaypointWorld == null) {
                waypointWorldContainer.worlds.put("waypoints", new WaypointWorld(waypointWorldContainer, "waypoints"));
                crossDimWaypointWorld = waypointWorldContainer.worlds.get("waypoints");
            }
            ArrayList<Waypoint> crossDimWaypoints = crossDimWaypointWorld.getSets().get(
                "gui.xaero_default").getList();
            crossDimWaypoints.removeIf(waypoint -> waypoint.isTemporary() && waypoint.getName().equals(waystone.name() + " [Waystone]"));
            Waypoint waystoneWp = new Waypoint(
                waystone.x(),
                waystone.y(),
                waystone.z(),
                waystone.name() + " [Waystone]",
                waystone.name().substring(0, 1).toUpperCase(Locale.ROOT),
                Math.abs(waystone.name().hashCode()) % COLORS.length,
                0,
                true
            );
            ((IWaypointDimension) waystoneWp).setDimension(waystoneDimension);
            crossDimWaypoints.add(waystoneWp);
        } catch (final Exception e) {
            // this most likely will be due to custom dimensions. Setting this down to debug so we don't spam logs in that case
            XaeroPlus.LOGGER.error("Failed to sync cross-dim waystone: {}", waystone.name(), e);
        }
    }

    private void currentDimWaystoneSync(Waystone waystone, WaypointsManager waypointsManager, List<Waypoint> currentWaypointSet) {
        try {
            final RegistryKey<World> waystoneDimension = waystone.dimension();
            final double dimDiv = getDimensionDivision(waystoneDimension, waypointsManager);
            final int x = OptimizedMath.myFloor(waystone.x() * dimDiv);
            final int z = OptimizedMath.myFloor(waystone.z() * dimDiv);
            Waypoint waypoint = new Waypoint(
                x,
                waystone.y(),
                z,
                waystone.name() + " [Waystone]",
                waystone.name().isEmpty() ? "W" : waystone.name()
                    .substring(0, 1)
                    .toUpperCase(Locale.ROOT),
                Math.abs(waystone.name().hashCode()) % COLORS.length,
                0,
                true
            );
            ((IWaypointDimension) waypoint).setDimension(waystoneDimension);
            currentWaypointSet.add(waypoint);
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Failed to sync waystone: {}", waystone.name(), e);
        }
    }

    private double getDimensionDivision(final RegistryKey<World> waystoneDim, final WaypointsManager waypointsManager) {
        String currentContainerID = waypointsManager.getCurrentContainerID();
        String dimPart = currentContainerID.substring(currentContainerID.lastIndexOf(47) + 1);
        RegistryKey<World> waypointContainerDim = waypointsManager.getDimensionKeyForDirectoryName(dimPart);
        if (waystoneDim == waypointContainerDim) return 1.0;
        double waypointsContainerDimDiv = waypointContainerDim == NETHER ? 8.0 : 1.0;
        double waystoneDimDiv = waystoneDim == NETHER ? 8.0 : 1.0;
        return waystoneDimDiv / waypointsContainerDimDiv;
    }

    private record Waystone(String name, RegistryKey<World> dimension, int x, int y, int z) { }
}
