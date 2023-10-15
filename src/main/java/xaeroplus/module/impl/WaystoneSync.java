package xaeroplus.module.impl;

import com.collarmc.pounce.Subscribe;
import com.google.common.hash.Hashing;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.waystones.api.IWaystone;
import net.blay09.mods.waystones.api.KnownWaystonesEvent;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import xaero.common.XaeroMinimapSession;
import xaero.common.minimap.waypoints.*;
import xaero.map.mods.SupportMods;
import xaeroplus.XaeroPlus;
import xaeroplus.event.ClientTickEvent;
import xaeroplus.event.XaeroWorldChangeEvent;
import xaeroplus.module.Module;
import xaeroplus.util.ColorHelper.WaystoneColor;
import xaeroplus.util.IWaypointDimension;
import xaeroplus.util.WaypointsHelper;
import xaeroplus.util.WaystonesHelper;

import java.util.*;
import java.util.stream.Collectors;

import static xaero.common.settings.ModSettings.COLORS;

@Module.ModuleInfo()
public class WaystoneSync extends Module {
    private boolean subscribed = false;
    private boolean shouldSync = false;
    // standard waystones we need to resync
    private List<IWaystone> toSyncWaystones = new ArrayList<>();
    // cache of currently synced standard waystones
    private List<IWaystone> currentWaystones = new ArrayList<>();
    private WaystoneColor color = WaystoneColor.RANDOM;
    private boolean separateWaypointSet = false;

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
        currentWaystones = event.getWaystones();
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
        }
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
        try {
            XaeroMinimapSession minimapSession = XaeroMinimapSession.getCurrentSession();
            if (minimapSession == null) return false;
            final WaypointsManager waypointsManager = minimapSession.getWaypointsManager();
            WaypointSet waypointSet = waypointsManager.getWaypoints();
            if (waypointSet == null) return false;
            final String currentContainerId = waypointsManager.getCurrentContainerID();

            // iterate over ALL waypoint sets and lists and remove waystones
            // todo: this doesn't iterate over dims/set permutations where we have no waystones at all
            //  there isn't a great interface xaero provides to get all permutations unfortunately - this already has lots of hacks
            final Map<Waystone, List<Waypoint>> waypointToWaypointsList = waystones.stream()
                .collect(Collectors.toMap((k1) -> k1,
                                          (v1) -> getWaypointsList(v1, waypointsManager, currentContainerId),
                                          (v1, v2) -> v1));
            for (List<Waypoint> waypointsList : new HashSet<>(waypointToWaypointsList.values())) {
                waypointsList.removeIf(waypoint -> waypoint.isTemporary() && waypoint.getName().endsWith(" [Waystone]"));
            }
            for (Map.Entry<Waystone, List<Waypoint>> entry : waypointToWaypointsList.entrySet()) {
                try {
                    waypointsListSync(entry.getKey(), entry.getValue());
                } catch (final Exception e) {
                    XaeroPlus.LOGGER.error("Error syncing waystone: " + entry.getKey().name(), e);
                }
            }
            SupportMods.xaeroMinimap.requestWaypointsRefresh();
            return true;
        } catch (Exception e) {
            XaeroPlus.LOGGER.error("Error syncing waystones", e);
            return true; // stops immediate retry. we'll still spam logs on the next iteration though
        }
    }

    private void waypointsListSync(final Waystone waystone, final List<Waypoint> waypointsList) {
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
        ((IWaypointDimension) waystoneWp).setDimension(waystone.dimension());
        waypointsList.add(waystoneWp);
    }

    private List<Waypoint> getWaypointsList(final Waystone waystone,
                                            final WaypointsManager waypointsManager,
                                            final String currentContainerId) {
        final RegistryKey<World> waystoneDimension = waystone.dimension();
        final String waystoneDimensionDirectoryName = waypointsManager.getDimensionDirectoryName(waystoneDimension);
        final int waystoneDim = WaypointsHelper.getDimensionForWaypointWorldKey(waystoneDimensionDirectoryName);
        final String worldContainerSuffix;
        if (waystoneDim == Integer.MIN_VALUE) // non-vanilla dimensions
            worldContainerSuffix = waystoneDimension.getValue().getNamespace() + "$" + waystoneDimension.getValue().getPath().replace("/", "%");
        else
            worldContainerSuffix = String.valueOf(waystoneDim);
        final WaypointWorldContainer waypointWorldContainer = waypointsManager.getWorldContainer(currentContainerId.substring(
            0,
            currentContainerId.lastIndexOf(37) + 1) + worldContainerSuffix);;
        WaypointWorld crossDimWaypointWorld = waypointWorldContainer.worlds.get("waypoints");
        if (crossDimWaypointWorld == null) {
            waypointWorldContainer.worlds.put("waypoints", new WaypointWorld(waypointWorldContainer, "waypoints"));
            crossDimWaypointWorld = waypointWorldContainer.worlds.get("waypoints");
        }
        final String waypointSetName = this.separateWaypointSet ? "Waystones" : "gui.xaero_default";
        WaypointSet waypointSet = crossDimWaypointWorld.getSets().get(waypointSetName);
        if (waypointSet == null) {
            crossDimWaypointWorld.getSets().put(waypointSetName, new WaypointSet(waypointSetName));
            waypointSet = crossDimWaypointWorld.getSets().get(waypointSetName);
        }
        return waypointSet.getList();
    }

    private int getWaystoneColor(Waystone waystone) {
        if (color == WaystoneColor.RANDOM) {
            return Math.abs(Hashing. murmur3_128().hashUnencodedChars(waystone.name()).asInt()) % COLORS.length;
        } else {
            return color.getColorIndex();
        }
    }

    public void setColor(final WaystoneColor color) {
        this.color = color;
        reloadStandardWaystones();
    }

    public void setWaypointSet(final boolean waypointSet) {
        this.separateWaypointSet = waypointSet;
        reloadStandardWaystones();
    }

    public void reloadStandardWaystones() {
        this.toSyncWaystones = this.currentWaystones;
    }

    private record Waystone(String name, RegistryKey<World> dimension, int x, int y, int z) { }
}
