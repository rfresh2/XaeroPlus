package xaeroplus.fabric.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import wraith.fwaystones.FabricWaystones;
import wraith.fwaystones.access.WaystoneValue;
import wraith.fwaystones.integration.event.WaystoneEvents;
import xaeroplus.module.impl.WaystoneSync;
import xaeroplus.util.IWraithWaystonesHelper;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class WraithWaystonesHelperImpl implements IWraithWaystonesHelper {
    private volatile boolean shouldSync = false;

    public WraithWaystonesHelperImpl() {
        WaystoneEvents.FORGET_ALL_WAYSTONES_EVENT.register((p) -> onWaystoneUpdate(null));
        WaystoneEvents.DISCOVER_WAYSTONE_EVENT.register(this::onWaystoneUpdate);
        WaystoneEvents.REMOVE_WAYSTONE_EVENT.register(this::onWaystoneUpdate);
        WaystoneEvents.RENAME_WAYSTONE_EVENT.register(this::onWaystoneUpdate);
    }

    @Override
    public boolean shouldSync() {
        return shouldSync;
    }

    @Override
    public void setShouldSync(final boolean shouldSync) {
        this.shouldSync = shouldSync;
    }

    @Override
    public List<WaystoneSync.Waystone> getWaystones() {
        var waystoneStorage = FabricWaystones.WAYSTONE_STORAGE;
        if (waystoneStorage == null) return Collections.emptyList();
        ConcurrentHashMap<String, WaystoneValue> waystones = waystoneStorage.WAYSTONES;
        if (waystones == null) return Collections.emptyList();
        return waystones.values().stream()
            .map(waystone -> new WaystoneSync.Waystone(
                waystone.getWaystoneName(),
                ResourceKey.create(Registries.DIMENSION, new ResourceLocation(waystone.getWorldName())),
                waystone.way_getPos().getX(),
                waystone.way_getPos().getY() + 1,// avoid teleporting directly into the waystone
                waystone.way_getPos().getZ()
                // todo: try to map the color int to waypoint color enum
//                waystone.getColor()
            ))
            .toList();
    }

    void onWaystoneUpdate(final String hash) {
        shouldSync = true;
    }
}
