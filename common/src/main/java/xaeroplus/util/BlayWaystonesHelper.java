package xaeroplus.util;

import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.waystones.api.*;
import net.minecraft.resources.ResourceLocation;
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
            .map(waystone -> new WaystoneSync.Waystone(
                waystone.getName(),
                waystone.getDimension(),
                waystone.getPos().getX(),
                waystone.getPos().getY() + 1,// avoid teleporting directly into the waystone
                waystone.getPos().getZ())).toList();
    }
}
