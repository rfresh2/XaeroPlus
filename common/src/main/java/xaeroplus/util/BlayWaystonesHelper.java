package xaeroplus.util;

import net.blay09.mods.waystones.api.SharestoneType;
import net.blay09.mods.waystones.api.Waystone;
import net.blay09.mods.waystones.api.WaystoneKinds;
import net.blay09.mods.waystones.api.event.WaystoneRemoveReceivedEvent;
import net.blay09.mods.waystones.api.event.WaystoneRemovedEvent;
import net.blay09.mods.waystones.api.event.WaystoneUpdateReceivedEvent;
import net.blay09.mods.waystones.api.event.WaystonesListReceivedEvent;
import net.minecraft.resources.Identifier;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaeroplus.module.impl.WaystoneSync;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BlayWaystonesHelper {
    public Map<Identifier, ArrayList<Waystone>> currentWaystoneTypeMap = new ConcurrentHashMap<>();
    public boolean shouldSync = false;
    private boolean subscribed = false;
    public final Object lock = new Object();

    public void subscribeWaystonesEvent() {
        if (subscribed) return;
        WaystonesListReceivedEvent.EVENT.register(this::onWaystonesListReceivedEvent);
        WaystoneUpdateReceivedEvent.EVENT.register(this::onWaystoneUpdateReceived);
        WaystoneRemoveReceivedEvent.EVENT.register(this::onWaystoneRemoveReceived);
        WaystoneRemovedEvent.EVENT.register(this::onWaystoneRemoved);
        subscribed = true;
    }

    private void onWaystoneRemoved(WaystoneRemovedEvent event) {
        if (isCompatibleWaystoneType(event.waystone().getWaystoneKind())) {
            synchronized (lock) {
                ArrayList<Waystone> waystones = currentWaystoneTypeMap.get(event.waystone().getWaystoneKind());
                if (waystones == null) return;
                waystones.removeIf(waystone -> waystone.getWaystoneUid().equals(event.waystone().getWaystoneUid()));
                if (waystones.isEmpty()) {
                    currentWaystoneTypeMap.remove(event.waystone().getWaystoneKind());
                }
                shouldSync = true;
            }
        }
    }

    private void onWaystoneRemoveReceived(WaystoneRemoveReceivedEvent event) {
        if (isCompatibleWaystoneType(event.waystoneType())) {
            synchronized (lock) {
                ArrayList<Waystone> waystones = currentWaystoneTypeMap.get(event.waystoneType());
                if (waystones == null) return;
                waystones.removeIf(waystone -> waystone.getWaystoneUid().equals(event.waystoneId()));
                if (waystones.isEmpty()) {
                    currentWaystoneTypeMap.remove(event.waystoneType());
                }
                shouldSync = true;
            }
        }
    }

    private void onWaystoneUpdateReceived(WaystoneUpdateReceivedEvent event) {
        if (isCompatibleWaystoneType(event.waystone().getWaystoneKind())) {
            synchronized (lock) {
                ArrayList<Waystone> waystones = currentWaystoneTypeMap.compute(
                    event.waystone().getWaystoneKind(),
                    (key, value) -> value == null ? new ArrayList<>() : value
                );
                waystones.removeIf(waystone -> waystone.getWaystoneUid().equals(event.waystone().getWaystoneUid()));
                waystones.add(event.waystone());
                shouldSync = true;
            }
        }
    }

    private boolean isCompatibleWaystoneType(final Identifier waystoneType) {
        return WaystoneKinds.WAYSTONE.equals(waystoneType) || WaystoneKinds.isSharestone(waystoneType);
    }

    public void onWaystonesListReceivedEvent(final WaystonesListReceivedEvent event) {
        if (isCompatibleWaystoneType(event.waystoneType())) {
            synchronized (lock) {
                currentWaystoneTypeMap.put(event.waystoneType(), new ArrayList<>(event.waystones()));
                shouldSync = true;
            }
        }
    }

    public List<WaystoneSync.Waystone> getCurrentWaystones() {
        return currentWaystoneTypeMap.values().stream()
            .flatMap(Collection::stream)
            .map(waystone -> {
                WaypointColor color = null;
                if (WaystoneKinds.isSharestone(waystone.getWaystoneKind())) {
                    SharestoneType sharestoneType = SharestoneType.get(waystone.getWaystoneKind());
                    if (sharestoneType != null) {
                        var dyeColor = sharestoneType.color();
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
                return new WaystoneSync.Waystone(
                    waystone.getName().getString(),
                    waystone.getDimension(),
                    waystone.getPos().getX(),
                    waystone.getPos().getY() + 1,// avoid teleporting directly into the waystone
                    waystone.getPos().getZ(),
                    color
                );
            }).toList();
    }
}
