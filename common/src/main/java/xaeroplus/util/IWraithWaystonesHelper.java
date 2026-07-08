package xaeroplus.util;

import xaeroplus.module.impl.WaystoneSync;

import java.util.List;

public interface IWraithWaystonesHelper {
    boolean shouldSync();
    void setShouldSync(boolean shouldSync);
    List<WaystoneSync.Waystone> getWaystones();
}
