package xaeroplus.util;

import net.minecraft.util.ResourceLocation;
import xaero.map.MapProcessor;
import xaero.map.WorldMapSession;
import xaero.map.core.XaeroWorldMapCore;
import xaero.map.world.MapWorld;
import xaeroplus.XaeroPlus;
import xaeroplus.settings.Settings;
import xaeroplus.settings.XaeroPlusSetting;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipInputStream;

/**
 * static variables and functions to share or persist across mixins
 */
public class Globals {

    // Map gui follow mode
    public static boolean FOLLOW = false;
    // cache and only update this on new world loads
    public static boolean nullOverworldDimensionFolder = false;
    public static Settings.DataFolderResolutionMode dataFolderResolutionMode = Settings.DataFolderResolutionMode.IP;
    public static int getCurrentDimensionId() {
        try {
            final Integer dim = XaeroWorldMapCore.currentSession.getMapProcessor().getMapWorld().getCurrentDimensionId();
            if (dim == null) return 0;
            else return dim;
        } catch (final Exception e) {
            XaeroPlus.LOGGER.info("Failed to get current dimension id", e);
            return 0;
        }
    }
    public static ExecutorService cacheRefreshExecutorService = Executors.newFixedThreadPool(
            // limited benefits by refreshing on more threads as it will consume the entire CPU and start lagging the game
            Math.max(1, Math.min(Runtime.getRuntime().availableProcessors() / 2, 4)));
    public static final ResourceLocation xpGuiTextures = new ResourceLocation("xaeroplus", "gui/xpgui.png");

    public static void onAllSettingsLoaded() {
        Settings.REGISTRY.getAllSettings().forEach(XaeroPlusSetting::init);
        nullOverworldDimensionFolder = Settings.REGISTRY.nullOverworldDimensionFolder.getValue();
        dataFolderResolutionMode = Settings.REGISTRY.dataFolderResolutionMode.getValue();
    }

    public static void switchToDimension(final int newDimId) {
        try {
            final WorldMapSession session = XaeroWorldMapCore.currentSession;
            if (session == null) return;
            final MapProcessor mapProcessor = session.getMapProcessor();
            if (mapProcessor == null) return;
            final MapWorld mapWorld = mapProcessor.getMapWorld();
            if (mapWorld == null) return;
            mapWorld.setCustomDimensionId(newDimId);
            mapProcessor.checkForWorldUpdate();
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Failed switching to dimension: {}", newDimId, e);
        }
    }

    public static byte[] decompressZipToBytes(final Path input) {
        try {
            return toUnzippedByteArray(Files.readAllBytes(input));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] toUnzippedByteArray(byte[] zippedBytes) throws IOException {
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(zippedBytes))) {
            final byte[] buff = new byte[1024];
            if (zipInputStream.getNextEntry() != null) {
                final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                int l;
                while ((l = zipInputStream.read(buff)) > 0) {
                    outputStream.write(buff, 0, l);
                }
                return outputStream.toByteArray();
            }
        } catch (final Throwable ignored) {
        }
        return new byte[0];
    }
}
