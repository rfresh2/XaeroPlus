package xaeroplus.util;

import baritone.api.utils.BetterBlockPos;
import xaeroplus.XaeroPlus;

public class BaritoneHelper {
    private static boolean isBaritonePresent = false;
    private static boolean checked = false;

    public static boolean isBaritonePresent() {
        if (!checked) {
            try {
                Class.forName(BetterBlockPos.class.getName());
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
