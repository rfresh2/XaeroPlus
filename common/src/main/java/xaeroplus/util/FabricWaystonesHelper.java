package xaeroplus.util;

import xaeroplus.module.impl.WaystoneSync;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

// Need to move out code that references FabricWaystones classes due to EventBus classloading shenanigans
public class FabricWaystonesHelper {

    public static Supplier<List<WaystoneSync.Waystone>> waystoneProvider;

    public static List<WaystoneSync.Waystone> getWaystones() {
        final var provider = waystoneProvider;
        if (provider != null)
            return provider.get();
        else return Collections.emptyList();
    }
}
