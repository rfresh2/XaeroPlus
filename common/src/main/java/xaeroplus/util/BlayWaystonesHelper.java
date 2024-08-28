package xaeroplus.util;

import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.waystones.api.IWaystone;
import net.blay09.mods.waystones.api.KnownWaystonesEvent;
import xaeroplus.module.impl.WaystoneSync;

import java.util.ArrayList;
import java.util.List;

public class BlayWaystonesHelper {
    public List<IWaystone> toSyncWaystones = new ArrayList<>();
    // cache of currently synced standard waystones
    public List<IWaystone> currentWaystones = new ArrayList<>();
    public boolean shouldSync = false;
    private boolean subscribed = false;

    public void subscribeWaystonesEvent() {
        if (subscribed) return;
        Balm.getEvents().onEvent(KnownWaystonesEvent.class, this::onKnownWaystonesEvent);
        subscribed = true;
    }

    public void onKnownWaystonesEvent(final KnownWaystonesEvent event) {
        toSyncWaystones = event.getWaystones();
        currentWaystones = event.getWaystones();
        shouldSync = true;
    }

    public List<WaystoneSync.Waystone> getToSyncWaystones() {
        return toSyncWaystones.stream()
            .map(waystone -> new WaystoneSync.Waystone(
                waystone.getName(),
                waystone.getDimension(),
                waystone.getPos().getX(),
                waystone.getPos().getY() + 1,// avoid teleporting directly into the waystone
                waystone.getPos().getZ())).toList();
    }
}
