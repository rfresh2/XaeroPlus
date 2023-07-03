package xaeroplus.util;

import baritone.api.BaritoneAPI;
import xaeroplus.XaeroPlus;

public class BaritoneHelper {
    private static boolean isBaritonePresent = false;
    private static boolean checked = false;

    public static boolean isBaritonePresent() {
        if (!checked) {
            try {
                Class.forName(BaritoneAPI.class.getName());
                XaeroPlus.LOGGER.info("Found Baritone API. Enabling Baritone support.");
                isBaritonePresent = true;
            } catch (final Throwable e) {
                XaeroPlus.LOGGER.info("Baritone API not found. Disabling Baritone support.");
                isBaritonePresent = false;
            }
            checked = true;
        }
        return isBaritonePresent;
    }
}
