package xaeroplus.util;

import xaeroplus.module.impl.WaystoneSync;

import java.util.List;

public class DefaultWraithWaystonesHelper implements IWraithWaystonesHelper {

    @Override
    public boolean shouldSync() {
        return false;
    }

    @Override
    public void setShouldSync(boolean shouldSync) {}

    @Override
    public List<WaystoneSync.Waystone> getWaystones() {
        return List.of();
    }
}
