package xaeroplus.util;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import org.waste.of.time.manager.CaptureManager;
import org.waste.of.time.storage.cache.HotCache;
import xaeroplus.XaeroPlus;

public class WorldToolsHelper {
    private static boolean isWorldToolsPresent = false;
    private static boolean checked = false;
    private static String minVersion = "1.2.0";

    public static boolean isWorldToolsPresent() {
        if (!checked) {
            try {
                Version worldtoolsVersion = FabricLoader.getInstance()
                    .getModContainer("worldtools")
                    .get()
                    .getMetadata()
                    .getVersion();
                var a = CaptureManager.INSTANCE.getCapturing();
                var b = HotCache.INSTANCE.isChunkSaved(0, 0);
                if (Version.parse(minVersion).compareTo(worldtoolsVersion) > 0) {
                    XaeroPlus.LOGGER.info("Incompatible WorldTools version. >={} required. Disabling WorldTools support. Found: {}", minVersion, worldtoolsVersion);
                    isWorldToolsPresent = false;
                } else {
                    XaeroPlus.LOGGER.info("Found WorldTools. Enabling WorldTools support.");
                    isWorldToolsPresent = true;
                }
            } catch (final Throwable e) {
                XaeroPlus.LOGGER.info("WorldTools not found. Disabling WorldTools support.");
                isWorldToolsPresent = false;
            }
            checked = true;
        }
        return isWorldToolsPresent;
    }

    public static boolean isDownloading() {
        return CaptureManager.INSTANCE.getCapturing();
    }
}
