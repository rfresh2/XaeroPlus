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

    public void subscribeWaystonesEvent() {
        Balm.getEvents().onEvent(KnownWaystonesEvent.class, this::onKnownWaystonesEvent);
    }

    public void onKnownWaystonesEvent(final KnownWaystonesEvent event) {
        toSyncWaystones = event.getWaystones();
        shouldSync = true;
        currentWaystones = event.getWaystones();
    }

    public List<WaystoneSync.Waystone> getToSyncWaystones() {
        return toSyncWaystones.stream()
            .map(waystone -> new WaystoneSync.Waystone(waystone.getName(),
                                                       waystone.getDimension(),
                                                       waystone.getPos().getX(),
                                                       waystone.getPos().getY() + 1,// avoid teleporting directly into the waystone
                                                       waystone.getPos().getZ())).toList();
    }
}
