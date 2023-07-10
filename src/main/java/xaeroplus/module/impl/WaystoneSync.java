package xaeroplus.module.impl;

import net.blay09.mods.waystones.client.ClientWaystones;
import net.blay09.mods.waystones.util.WaystoneActivatedEvent;
import net.blay09.mods.waystones.util.WaystoneEntry;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import xaero.common.XaeroMinimapSession;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.common.minimap.waypoints.WaypointSet;
import xaero.common.minimap.waypoints.WaypointsManager;
import xaero.common.misc.OptimizedMath;
import xaero.map.mods.SupportMods;
import xaeroplus.event.XaeroWorldChangeEvent;
import xaeroplus.module.Module;
import xaeroplus.util.WaystonesHelper;

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
        final WaypointSet waypointSet = waypointsManager.getWaypoints();
        if (waypointSet == null) return;
        final List<Waypoint> waypoints = waypointSet.getList();
        waypoints.removeIf(waypoint -> waypoint.isTemporary() && waypoint.getName().endsWith(" [Waystone]"));
        for (WaystoneEntry waystoneEntry : knownWaystones) {
            final double dimDiv = getDimensionDivision(waystoneEntry.getDimensionId(), waypointsManager);
            final int x = OptimizedMath.myFloor(waystoneEntry.getPos().getX() * dimDiv);
            final int z = OptimizedMath.myFloor(waystoneEntry.getPos().getZ() * dimDiv);
            waypoints.add(new Waypoint(
                    x,
                    waystoneEntry.getPos().getY(),
                    z,
                    waystoneEntry.getName() + " [Waystone]",
                    waystoneEntry.getName().substring(0, 1).toUpperCase(Locale.ROOT),
                    Math.abs(waystoneEntry.getName().hashCode()) % COLORS.length,
                    0,
                    true
            ));
        }
        SupportMods.xaeroMinimap.requestWaypointsRefresh();
    }

    private double getDimensionDivision(final int waystoneDim, final WaypointsManager waypointsManager) {
        String currentContainerID = waypointsManager.getCurrentContainerID();
        String dimPart = currentContainerID.substring(currentContainerID.lastIndexOf(47) + 1);
        Integer waypointContainerDim = waypointsManager.getDimensionForDirectoryName(dimPart);
        if (waystoneDim == waypointContainerDim) return 1.0;
        double waypointsContainerDimDiv = waypointContainerDim == -1 ? 8.0 : 1.0;
        double waystoneDimDiv = waystoneDim == -1 ? 8.0 : 1.0;
        return waystoneDimDiv / waypointsContainerDimDiv;
    }
}
