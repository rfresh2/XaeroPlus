package xaeroplus.util;

import baritone.api.BaritoneAPI;
import baritone.api.process.IElytraProcess;
import xaeroplus.XaeroPlus;

public final class BaritoneHelper {
    private BaritoneHelper() {}
    private static boolean isBaritonePresent = false;
    private static boolean isBaritoneElytraPresent = false;
    private static boolean checkedBaritone = false;
    private static boolean checkedElytra = false;

    public static boolean isBaritonePresent() {
        if (!checkedBaritone) {
            try {
                Class.forName(BaritoneAPI.class.getName());
                XaeroPlus.LOGGER.info("Found Baritone API. Enabling Baritone support.");
                isBaritonePresent = true;
            } catch (final Throwable e) {
                XaeroPlus.LOGGER.info("Baritone API not found. Disabling Baritone support.");
                isBaritonePresent = false;
            }
            checkedBaritone = true;
        }
        return isBaritonePresent;
    }

    public static boolean isBaritoneElytraPresent() {
        if (!checkedElytra) {
            try {
                Class.forName(IElytraProcess.class.getName());
                XaeroPlus.LOGGER.info("Found Baritone Elytra API. Enabling Baritone Elytra support.");
                isBaritoneElytraPresent = true;
            } catch (Throwable e) {
                XaeroPlus.LOGGER.info("Baritone Elytra API not found. Disabling Baritone Elytra support.");
                isBaritoneElytraPresent = false;
            }
            checkedElytra = true;
        }
        return isBaritoneElytraPresent;
    }
}
