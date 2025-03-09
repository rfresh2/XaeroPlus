package xaeroplus.module.impl;

import com.google.common.hash.Hashing;
import net.lenni0451.lambdaevents.EventHandler;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.common.minimap.waypoints.WaypointVisibilityType;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.hud.minimap.waypoint.WaypointPurpose;
import xaero.hud.minimap.waypoint.set.WaypointSet;
import xaero.hud.minimap.world.MinimapWorld;
import xaero.hud.minimap.world.MinimapWorldManager;
import xaeroplus.XaeroPlus;
import xaeroplus.event.ClientTickEvent;
import xaeroplus.event.XaeroWorldChangeEvent;
import xaeroplus.feature.waypoint.WaypointAPI;
import xaeroplus.module.Module;
import xaeroplus.settings.Settings;
import xaeroplus.util.BlayWaystonesHelper;
import xaeroplus.util.ColorHelper.WaystoneColor;
import xaeroplus.util.FabricWaystonesHelper;
import xaeroplus.util.WaystonesHelper;

import java.util.List;
import java.util.Locale;

import static xaeroplus.event.XaeroWorldChangeEvent.WorldChangeType.ENTER_WORLD;
import static xaeroplus.event.XaeroWorldChangeEvent.WorldChangeType.EXIT_WORLD;

public class WaystoneSync extends Module {
    private final BlayWaystonesHelper blayWaystonesHelper = new BlayWaystonesHelper();
    private WaystoneColor color = WaystoneColor.RANDOM;
    private boolean separateWaypointSet = false;
    private WaypointVisibilityType visibilityType = WaypointVisibilityType.LOCAL;

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
        if (event.worldChangeType() == EXIT_WORLD || event.worldChangeType() == ENTER_WORLD) {
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
            clearWaystoneWaypoints();
            for (Waystone waystone : waystones) {
                try {
                    waypointsListSync(waystone, getWaypointSet(waystone));
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
    private void clearWaystoneWaypoints() {
        WaypointAPI.forEachWaypointSetInCurrentContainer(waypoints -> {
            waypoints.removeIf(WaystoneSync::isWaystoneWaypoint);
        });
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
            WaypointPurpose.NORMAL,
            true
        );
        waystoneWp.setVisibility(visibilityType);
        waypointsList.add(waystoneWp);
    }

    private WaypointSet getWaypointSet(final Waystone waystone) {
        final String waypointSetName = this.separateWaypointSet ? "Waystones" : "gui.xaero_default";
        final MinimapWorld waypointWorld = getWaypointWorldForWaystone(waystone);
        return WaypointAPI.getOrCreateWaypointSetInWorld(waypointWorld, waypointSetName);
    }

    private MinimapWorld getWaypointWorldForWaystone(final Waystone waystone) {
        MinimapWorld waystoneWpMinimapWorld = WaypointAPI.getMinimapWorld(waystone.dimension);
        if (waystoneWpMinimapWorld != null) {
            return waystoneWpMinimapWorld;
        } else {
            throw new RuntimeException("WaystoneSync: waystone world is null");
        }
    }

    private WaypointColor getWaystoneColor(Waystone waystone) {
        if (color == WaystoneColor.RANDOM) {
            int index = Math.abs(
                Hashing.murmur3_128().hashUnencodedChars(waystone.name()).asInt())
                % WaypointColor.values().length;
            return WaypointColor.fromIndex(index);
        } else {
            return WaypointColor.fromIndex(color.getColorIndex());
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

    public void setVisibilityType(final Settings.WaystoneWpVisibilityType visibilityType) {
        this.visibilityType = switch (visibilityType) {
            case LOCAL -> WaypointVisibilityType.LOCAL;
            case GLOBAL -> WaypointVisibilityType.GLOBAL;
            case WORLD_MAP_LOCAL -> WaypointVisibilityType.WORLD_MAP_LOCAL;
            case WORLD_MAP_GLOBAL -> WaypointVisibilityType.WORLD_MAP_GLOBAL;
        };
        reloadWaystones();
    }

    public void reloadWaystones() {
        blayWaystonesHelper.shouldSync = true;
        FabricWaystonesHelper.shouldSync = true;
    }

    public record Waystone(String name, ResourceKey<Level> dimension, int x, int y, int z) { }
}
