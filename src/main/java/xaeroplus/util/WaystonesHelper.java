package xaeroplus.util;

import net.blay09.mods.waystones.api.event.WaystonesListReceivedEvent;
import xaeroplus.XaeroPlus;

public class WaystonesHelper {
    private static boolean isWaystonesPresent = false;
    private static boolean checked = false;

    public static boolean isWaystonesPresent() {
        if (!checked) {
            try {
                // events changed in recent versions of Waystones
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
}
