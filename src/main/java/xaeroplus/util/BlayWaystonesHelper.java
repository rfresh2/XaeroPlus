package xaeroplus.util;

import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.waystones.api.Waystone;
import net.blay09.mods.waystones.api.WaystoneTypes;
import net.blay09.mods.waystones.api.event.WaystoneRemoveReceivedEvent;
import net.blay09.mods.waystones.api.event.WaystoneUpdateReceivedEvent;
import net.blay09.mods.waystones.api.event.WaystonesListReceivedEvent;
import net.minecraft.resources.ResourceLocation;
import xaeroplus.module.impl.WaystoneSync;

import java.util.ArrayList;
import java.util.List;

public class BlayWaystonesHelper {
    public List<Waystone> currentWaystones = new ArrayList<>();
    public boolean shouldSync = false;

    public void subscribeWaystonesEvent() {
        Balm.getEvents().onEvent(WaystonesListReceivedEvent.class, this::onWaystonesListReceivedEvent);
        Balm.getEvents().onEvent(WaystoneUpdateReceivedEvent.class, this::onWaystoneUpdateReceived);
        Balm.getEvents().onEvent(WaystoneRemoveReceivedEvent.class, this::onWaystoneRemoveReceived);
    }

    private void onWaystoneRemoveReceived(WaystoneRemoveReceivedEvent waystoneRemoveReceivedEvent) {
        if (isCompatibleWaystoneType(waystoneRemoveReceivedEvent.getWaystoneType())) {
            currentWaystones.removeIf(waystone -> waystone.getWaystoneUid().equals(waystoneRemoveReceivedEvent.getWaystoneId()));
            shouldSync = true;
        }
    }

    private void onWaystoneUpdateReceived(WaystoneUpdateReceivedEvent waystoneUpdateReceivedEvent) {
        if (isCompatibleWaystoneType(waystoneUpdateReceivedEvent.getWaystone().getWaystoneType())) {
            currentWaystones.removeIf(waystone -> waystone.getWaystoneUid().equals(waystoneUpdateReceivedEvent.getWaystone().getWaystoneUid()));
            currentWaystones.add(waystoneUpdateReceivedEvent.getWaystone());
            shouldSync = true;
        }
    }

    private boolean isCompatibleWaystoneType(final ResourceLocation waystoneType) {
        return waystoneType.equals(WaystoneTypes.WAYSTONE) || WaystoneTypes.isSharestone(waystoneType);
    }

    public void onWaystonesListReceivedEvent(final WaystonesListReceivedEvent event) {
        currentWaystones = event.getWaystones().stream().filter(waystone -> isCompatibleWaystoneType(waystone.getWaystoneType())).toList();
        shouldSync = true;
    }

    public List<WaystoneSync.Waystone> getCurrentWaystones() {
        return currentWaystones.stream()
            .map(waystone -> new WaystoneSync.Waystone(waystone.getName().getString(),
                                                       waystone.getDimension(),
                                                       waystone.getPos().getX(),
                                                       waystone.getPos().getY() + 1,// avoid teleporting directly into the waystone
                                                       waystone.getPos().getZ())).toList();
    }
}
