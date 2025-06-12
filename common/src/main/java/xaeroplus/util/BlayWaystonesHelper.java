package xaeroplus.util;

import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.waystones.api.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaeroplus.module.impl.WaystoneSync;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BlayWaystonesHelper {
    public Map<ResourceLocation, ArrayList<IWaystone>> currentWaystoneTypeMap = new ConcurrentHashMap<>();
    public boolean shouldSync = false;
    private boolean subscribed = false;
    public final Object lock = new Object();

    public void subscribeWaystonesEvent() {
        if (subscribed) return;
        Balm.getEvents().onEvent(WaystonesListReceivedEvent.class, this::onWaystonesListReceivedEvent);
        Balm.getEvents().onEvent(WaystoneUpdateReceivedEvent.class, this::onWaystoneUpdateReceived);
        Balm.getEvents().onEvent(WaystoneRemoveReceivedEvent.class, this::onWaystoneRemoveReceived);
        subscribed = true;
    }

    private void onWaystoneRemoveReceived(WaystoneRemoveReceivedEvent event) {
        if (isCompatibleWaystoneType(event.getWaystoneType())) {
            synchronized (lock) {
                ArrayList<IWaystone> waystones = currentWaystoneTypeMap.get(event.getWaystoneType());
                if (waystones == null) return;
                waystones.removeIf(waystone -> waystone.getWaystoneUid().equals(event.getWaystoneId()));
                if (waystones.isEmpty()) {
                    currentWaystoneTypeMap.remove(event.getWaystoneType());
                }
                shouldSync = true;
            }
        }
    }

    private void onWaystoneUpdateReceived(WaystoneUpdateReceivedEvent event) {
        if (isCompatibleWaystoneType(event.getWaystone().getWaystoneType())) {
            synchronized (lock) {
                ArrayList<IWaystone> waystones = currentWaystoneTypeMap.get(event.getWaystone().getWaystoneType());
                if (waystones == null) return;
                waystones.removeIf(waystone -> waystone.getWaystoneUid().equals(event.getWaystone().getWaystoneUid()));
                waystones.add(event.getWaystone());
                shouldSync = true;
            }
        }
    }

    private boolean isCompatibleWaystoneType(final ResourceLocation waystoneType) {
        return waystoneType.equals(WaystoneTypes.WAYSTONE) || WaystoneTypes.isSharestone(waystoneType);
    }

    public void onWaystonesListReceivedEvent(final WaystonesListReceivedEvent event) {
        if (isCompatibleWaystoneType(event.getWaystoneType())) {
            synchronized (lock) {
                currentWaystoneTypeMap.put(event.getWaystoneType(), new ArrayList<>(event.getWaystones()));
                shouldSync = true;
            }
        }
    }

    public List<WaystoneSync.Waystone> getCurrentWaystones() {
        return currentWaystoneTypeMap.values().stream()
            .flatMap(Collection::stream)
            .map(waystone -> {
                WaypointColor color = null;
                if (WaystoneTypes.isSharestone(waystone.getWaystoneType())) {
                    var keyPath = waystone.getWaystoneType().getPath();
                    int suffixIndex = keyPath.lastIndexOf("_sharestone");
                    if (suffixIndex != -1) {
                        String colorName = keyPath.substring(0, suffixIndex);
                        DyeColor dyeColor = DyeColor.byName(colorName, null);
                        if (dyeColor != null) {
                            color = switch (dyeColor) {
                                case WHITE -> WaypointColor.WHITE;
                                case ORANGE -> WaypointColor.GOLD;
                                case MAGENTA -> WaypointColor.DARK_PURPLE;
                                case LIGHT_BLUE -> WaypointColor.AQUA;
                                case YELLOW -> WaypointColor.YELLOW;
                                case LIME -> WaypointColor.GREEN;
                                case GREEN -> WaypointColor.DARK_GREEN;
                                case PINK-> WaypointColor.PURPLE;
                                case PURPLE -> WaypointColor.BLUE;
                                case GRAY ->  WaypointColor.DARK_GRAY;
                                case LIGHT_GRAY -> WaypointColor.GRAY;
                                case CYAN -> WaypointColor.DARK_AQUA;
                                case BLUE -> WaypointColor.DARK_BLUE;
                                case BROWN -> WaypointColor.DARK_RED;
                                case RED -> WaypointColor.RED;
                                case BLACK -> WaypointColor.BLACK;
                            };
                        }
                    }
                }
                return new WaystoneSync.Waystone(
                    waystone.getName(),
                    waystone.getDimension(),
                    waystone.getPos().getX(),
                    waystone.getPos().getY() + 1,// avoid teleporting directly into the waystone
                    waystone.getPos().getZ(),
                    color
                );
            }).toList();
    }
}
