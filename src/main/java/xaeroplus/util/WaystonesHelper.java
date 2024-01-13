package xaeroplus.util;

import net.blay09.mods.waystones.Waystones;
import net.blay09.mods.waystones.api.event.WaystonesListReceivedEvent;
import wraith.fwaystones.FabricWaystones;
import xaeroplus.XaeroPlus;

public class WaystonesHelper {
    private static boolean isWaystonesPresent = false;
    private static boolean isFabricWaystonesPresent = false;
    private static boolean checked = false;
    private static boolean fabricChecked = false;

    public static boolean isWaystonesPresent() {
        if (!checked) {
            try {
                // events changed in recent versions of Waystones
                Class.forName(Waystones.class.getName());
                Class.forName(WaystonesListReceivedEvent.class.getName());
                XaeroPlus.LOGGER.info("Found Waystones. Enabling Waystones support.");
                isWaystonesPresent = true;
            } catch (final Throwable e) {
                XaeroPlus.LOGGER.info("Waystones not found. Disabling Waystones support.");
                isWaystonesPresent = false;
            }
            checked = true;
        }
        return isWaystonesPresent;
    }

    public static boolean isFabricWaystonesPresent() {
        if (!fabricChecked) {
            try {
                Class.forName(FabricWaystones.class.getName());
                XaeroPlus.LOGGER.info("Found Fabric Waystones. Enabling Fabric Waystones support.");
                isFabricWaystonesPresent = true;
            } catch (final Throwable e) {
                XaeroPlus.LOGGER.info("Fabric Waystones not found. Disabling Fabric Waystones support.");
                isFabricWaystonesPresent = false;
            }
            fabricChecked = true;
        }
        return isFabricWaystonesPresent;
    }

    public static boolean isAnyWaystonesPresent() {
        return isWaystonesPresent() || isFabricWaystonesPresent();
    }

}
