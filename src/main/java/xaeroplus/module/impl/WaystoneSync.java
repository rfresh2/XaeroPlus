package xaeroplus.module.impl;

import net.blay09.mods.waystones.client.ClientWaystones;
import net.blay09.mods.waystones.util.WaystoneActivatedEvent;
import net.blay09.mods.waystones.util.WaystoneEntry;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import xaero.common.XaeroMinimapSession;
import xaero.common.minimap.waypoints.*;
import xaero.common.misc.OptimizedMath;
import xaero.map.mods.SupportMods;
import xaeroplus.XaeroPlus;
import xaeroplus.event.XaeroWorldChangeEvent;
import xaeroplus.module.Module;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.util.IWaypointDimension;
import xaeroplus.util.WaystonesHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static xaero.common.settings.ModSettings.COLORS;

@Module.ModuleInfo()
public class WaystoneSync extends Module {
    private static final long delay = 3000L;
    private long lastSync = 0L;

    @SubscribeEvent
    @Optional.Method(modid = "waystones")
    public void onClientTickEvent(final TickEvent.ClientTickEvent event) {
        // wouldn't need this tick event if we knew when waystones were removed
        if (event.phase != TickEvent.Phase.END) return;
        if (!WaystonesHelper.isWaystonesPresent()) return;
        if (System.currentTimeMillis() - lastSync < delay) return;
        syncWaystones();
    }

    @SubscribeEvent
    @Optional.Method(modid = "waystones")
    public void onWorldChangeEvent(XaeroWorldChangeEvent event) {
        syncWaystones();
    }

    @SubscribeEvent
    @Optional.Method(modid = "waystones")
    public void onWaystoneActivatedEvent(WaystoneActivatedEvent event) {
        syncWaystones();
    }

    private void syncWaystones() {
        lastSync = System.currentTimeMillis();
        WaystoneEntry[] knownWaystones = ClientWaystones.getKnownWaystones();
        XaeroMinimapSession minimapSession = XaeroMinimapSession.getCurrentSession();
        if (minimapSession == null) return;
        final WaypointsManager waypointsManager = minimapSession.getWaypointsManager();
        WaypointWorld currentWaypointWorld = waypointsManager.getCurrentWorld();
        if (currentWaypointWorld == null) return;
        final WaypointSet waypointSet = waypointsManager.getWaypoints();
        if (waypointSet == null) return;
        final List<Waypoint> waypoints = waypointSet.getList();
        waypoints.removeIf(waypoint -> waypoint.isTemporary() && waypoint.getName().endsWith(" [Waystone]"));
        for (WaystoneEntry waystoneEntry : knownWaystones) {
            final int waystoneDim = waystoneEntry.getDimensionId();

            final String currentContainerId = waypointsManager.getCurrentContainerID();
            final int currentWaypointDim = currentWaypointWorld.getDimId();
            if (XaeroPlusSettingRegistry.waystonesCrossDimSyncSetting.getValue() && waystoneDim != currentWaypointDim) { // special case handling
                // god i hate how there's no easy way to get waypoint sets in other dimensions. also the entire handling of waypoint dimensions...
                // this will probably (definitely) fail if there are nonstandard waypoint sets in use.
                try {
                    WaypointWorldContainer waypointWorldContainer = waypointsManager.getWorldContainer(currentContainerId.substring(
                            0,
                            currentContainerId.lastIndexOf(37) + 1) + waystoneDim);
                    WaypointWorld crossDimWaypointWorld = waypointWorldContainer.worlds.get("waypoints");
                    if (crossDimWaypointWorld == null) {
                        waypointWorldContainer.worlds.put("waypoints", new WaypointWorld(waypointWorldContainer, "waypoints", waystoneDim));
                        crossDimWaypointWorld = waypointWorldContainer.worlds.get("waypoints");
                    }
                    ArrayList<Waypoint> crossDimWaypoints = crossDimWaypointWorld.getSets().get(
                            "gui.xaero_default").getList();
                    // todo: support waystone removal sync for cross-dimension waystones
                    crossDimWaypoints.removeIf(waypoint -> waypoint.isTemporary() && waypoint.getName().equals(waystoneEntry.getName() + " [Waystone]"));
                    Waypoint waystoneWp = new Waypoint(
                        waystoneEntry.getPos().getX(),
                        waystoneEntry.getPos().getY(),
                        waystoneEntry.getPos().getZ(),
                        waystoneEntry.getName() + " [Waystone]",
                        waystoneEntry.getName().substring(0, 1).toUpperCase(Locale.ROOT),
                        Math.abs(waystoneEntry.getName().hashCode()) % COLORS.length,
                        0,
                        true
                    );
                    ((IWaypointDimension) waystoneWp).setDimension(waystoneDim);
                    crossDimWaypoints.add(waystoneWp);
                } catch (final Exception e) {
                    XaeroPlus.LOGGER.warn("Failed to add cross-dimension waystone", e);
                }
                continue;
            }

            // places all waystones in the current waypoint set regardless of dimension
            final double dimDiv = getDimensionDivision(waystoneEntry.getDimensionId(), currentWaypointWorld);
            final int x = OptimizedMath.myFloor(waystoneEntry.getPos().getX() * dimDiv);
            final int z = OptimizedMath.myFloor(waystoneEntry.getPos().getZ() * dimDiv);
            Waypoint waystoneWp = new Waypoint(
                x,
                waystoneEntry.getPos().getY(),
                z,
                waystoneEntry.getName() + " [Waystone]",
                waystoneEntry.getName().substring(0, 1).toUpperCase(Locale.ROOT),
                Math.abs(waystoneEntry.getName().hashCode()) % COLORS.length,
                0,
                true
            );
            ((IWaypointDimension) waystoneWp).setDimension(waystoneEntry.getDimensionId());
            waypoints.add(waystoneWp);
        }
        SupportMods.xaeroMinimap.requestWaypointsRefresh();
    }

    private double getDimensionDivision(final int waystoneDim, final WaypointWorld currentWaypointWorld) {
        int waypointContainerDim = currentWaypointWorld.getDimId();
        if (waystoneDim == waypointContainerDim) return 1.0;
        double waypointsContainerDimDiv = waypointContainerDim == -1 ? 8.0 : 1.0;
        double waystoneDimDiv = waystoneDim == -1 ? 8.0 : 1.0;
        return waystoneDimDiv / waypointsContainerDimDiv;
    }
}
