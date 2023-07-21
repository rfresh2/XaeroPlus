package xaeroplus.module.impl;

import com.collarmc.pounce.Subscribe;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.waystones.api.IWaystone;
import net.blay09.mods.waystones.api.KnownWaystonesEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import xaero.common.XaeroMinimapSession;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.common.minimap.waypoints.WaypointSet;
import xaero.common.minimap.waypoints.WaypointsManager;
import xaero.common.misc.OptimizedMath;
import xaero.map.mods.SupportMods;
import xaeroplus.XaeroPlus;
import xaeroplus.event.ClientTickEvent;
import xaeroplus.event.XaeroWorldChangeEvent;
import xaeroplus.module.Module;
import xaeroplus.settings.XaeroPlusSettingRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static net.minecraft.world.World.NETHER;
import static xaero.common.settings.ModSettings.COLORS;

@Module.ModuleInfo()
public class WaystoneSync extends Module {
    private boolean subscribed = false;
    private boolean shouldSync = false;
    private List<IWaystone> toSyncWaystones = new ArrayList<>();

    @Override
    public void onEnable() {
        if (!subscribed) {
            subscribed = true;
            Balm.getEvents().onEvent(KnownWaystonesEvent.class, this::onKnownWaystonesEvent);
        }
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
        if (shouldSync) {
            if (syncWaypoints()) {
                shouldSync = false;
                toSyncWaystones = new ArrayList<>();
            }
        }
    }

    public boolean syncWaypoints() {
        XaeroMinimapSession minimapSession = XaeroMinimapSession.getCurrentSession();
        if (minimapSession == null) return false;
        final WaypointsManager waypointsManager = minimapSession.getWaypointsManager();
        WaypointSet waypointSet = waypointsManager.getWaypoints();
        if (waypointSet == null) return false;
        final List<Waypoint> waypoints = waypointSet.getList();
        waypoints.removeIf(waypoint -> waypoint.isTemporary() && waypoint.getName().endsWith(" [Waystone]"));
        for (IWaystone waystoneEntry : toSyncWaystones) {
            XaeroPlus.LOGGER.info("Syncing waystone entry: {} pos: {}", waystoneEntry.getName(), waystoneEntry.getPos().toString());
            final double dimDiv = getDimensionDivision(waystoneEntry.getDimension(), waypointsManager);
            final int x = OptimizedMath.myFloor(waystoneEntry.getPos().getX() * dimDiv);
            final int z = OptimizedMath.myFloor(waystoneEntry.getPos().getZ() * dimDiv);
            waypoints.add(new Waypoint(
                    x,
                    waystoneEntry.getPos().getY(),
                    z,
                    waystoneEntry.getName() + " [Waystone]",
                    waystoneEntry.getName().isEmpty() ? "W" : waystoneEntry.getName().substring(0, 1).toUpperCase(Locale.ROOT),
                    Math.abs(waystoneEntry.getName().hashCode()) % COLORS.length,
                    0,
                    true
            ));
        }
        SupportMods.xaeroMinimap.requestWaypointsRefresh();
        return true;
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
}
