package xaeroplus.util;

import xaero.map.mods.pac.gui.PlayerDynamicInfoMapElement;

public final class PacHelper {

    public static int getSyncableX(final PlayerDynamicInfoMapElement syncable) {
        return (int) syncable.getSyncable().getX();
    }

    public static int getSyncableZ(final PlayerDynamicInfoMapElement syncable) {
        return (int) syncable.getSyncable().getZ();
    }
}
