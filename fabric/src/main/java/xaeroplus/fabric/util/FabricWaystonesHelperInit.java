package xaeroplus.fabric.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import wraith.fwaystones.FabricWaystones;
import wraith.fwaystones.access.WaystoneValue;
import wraith.fwaystones.integration.event.WaystoneEvents;
import xaeroplus.module.impl.WaystoneSync;
import xaeroplus.util.FabricWaystonesHelper;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class FabricWaystonesHelperInit {

    public static void doInit() {
        FabricWaystonesHelper.waystoneProvider = FabricWaystonesHelperInit::getWaystones;
        FabricWaystonesHelper.subcribeWaystonesEventsRunnable = FabricWaystonesHelperInit::subscribeWaystonesEvents;
    }

    public static void subscribeWaystonesEvents() {
        if (FabricWaystonesHelper.subscribed) return;
        WaystoneEvents.FORGET_ALL_WAYSTONES_EVENT.register((p) -> FabricWaystonesHelper.onWaystoneUpdate(null));
        WaystoneEvents.DISCOVER_WAYSTONE_EVENT.register(FabricWaystonesHelper::onWaystoneUpdate);
        WaystoneEvents.REMOVE_WAYSTONE_EVENT.register(FabricWaystonesHelper::onWaystoneUpdate);
        WaystoneEvents.RENAME_WAYSTONE_EVENT.register(FabricWaystonesHelper::onWaystoneUpdate);
        FabricWaystonesHelper.subscribed = true;
    }

    public static List<WaystoneSync.Waystone> getWaystones() {
        var waystoneStorage = FabricWaystones.WAYSTONE_STORAGE;
        if (waystoneStorage == null) return Collections.emptyList();
        ConcurrentHashMap<String, WaystoneValue> waystones = waystoneStorage.WAYSTONES;
        if (waystones == null) return Collections.emptyList();
        return waystones.values().stream()
            .map(waystone -> new WaystoneSync.Waystone(
                waystone.getWaystoneName(),
                ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(waystone.getWorldName())),
                waystone.way_getPos().getX(),
                waystone.way_getPos().getY() + 1,// avoid teleporting directly into the waystone
                waystone.way_getPos().getZ()))
            .toList();
    }
}
