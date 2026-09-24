package xaeroplus.util;

import baritone.api.BaritoneAPI;
import baritone.api.process.IElytraProcess;
import baritone.process.ElytraProcess;
import baritone.process.elytra.ElytraBehavior;
import baritone.process.elytra.NetherPath;
import xaeroplus.XaeroPlus;

import java.util.List;

public final class BaritoneHelper {
    private BaritoneHelper() {}
    private static boolean isBaritonePresent = false;
    private static boolean isBaritoneElytraPresent = false;
    private static boolean checkedBaritone = false;
    private static boolean checkedElytra = false;
    private static boolean deobf = false;
    private static boolean checkedDeobf = false;
    private static boolean elytraBehavior = false;
    private static boolean checkedElytraBehavior = false;
    private static boolean checkedElytraPathAPI = false;
    private static boolean elytraPathAPIPresent = false;

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

    public static boolean isBaritoneDeobf() {
        if (!checkedDeobf) {
            try {
                Class.forName("baritone.Baritone");
                XaeroPlus.LOGGER.info("Detected deobf baritone build");
                deobf = true;
            } catch (Throwable e) {
                XaeroPlus.LOGGER.info("Detected obfuscated baritone build");
                deobf = false;
            }
            checkedDeobf = true;
        }
        return deobf;
    }

    public static boolean isElytraPathAPIPresent() {
        if (!checkedElytraPathAPI) {
            try {
                var getPathMethod = IElytraProcess.class.getDeclaredMethod("getPath");
                if (getPathMethod.getReturnType() != List.class) {
                    throw new RuntimeException("Bad return type: " + getPathMethod.getReturnType().getName());
                }
                elytraPathAPIPresent = true;
                XaeroPlus.LOGGER.info("Baritone elytra path API found");

            } catch (Throwable e) {
                XaeroPlus.LOGGER.error("Baritone elytra path API not found, pls update baritone to fix!", e);
                elytraPathAPIPresent = false;
            }
            checkedElytraPathAPI = true;
        }
        return elytraPathAPIPresent;
    }

    public static boolean isElytraPathReflectionAccessible() {
        if (!checkedElytraBehavior) {
            try {
                var elytraProcessClass = Class.forName(ElytraProcess.class.getName());
                var behaviorField = elytraProcessClass.getDeclaredField("behavior");
                if (behaviorField.getType() != ElytraBehavior.class) {
                    throw new RuntimeException("incorrect elytra behavior field type");
                }
                behaviorField.setAccessible(true);
                var behaviorPathManagerField = ElytraBehavior.class.getDeclaredField("pathManager");
                if (behaviorPathManagerField.getType() != ElytraBehavior.PathManager.class) {
                    throw new RuntimeException("incorrect elytra path manager field type");
                }
                behaviorPathManagerField.setAccessible(true);
                var netherPathField = ElytraBehavior.PathManager.class.getDeclaredField("path");
                if (netherPathField.getType() != NetherPath.class) {
                    throw new RuntimeException("incorrect nether path field type");
                }
                netherPathField.setAccessible(true);
                if (!List.class.isAssignableFrom(NetherPath.class)) {
                    throw new RuntimeException("NetherPath not a list");
                }
                var pathListGetMethod = NetherPath.class.getDeclaredMethod("get", int.class);
                var pathSizeMethod = NetherPath.class.getDeclaredMethod("size");
                XaeroPlus.LOGGER.info("Baritone elytra path reflection accessible");
                elytraBehavior = true;
            } catch (Exception e) {
                XaeroPlus.LOGGER.error("Baritone elytra path reflection not accessible", e);
                elytraBehavior = false;
            }
            checkedElytraBehavior = true;
        }
        return elytraBehavior;
    }
}
