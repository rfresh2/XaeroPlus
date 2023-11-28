package xaeroplus.util;

import com.google.common.collect.Lists;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;
import xaero.map.MapProcessor;
import xaero.map.WorldMapSession;
import xaero.map.core.XaeroWorldMapCore;
import xaero.map.world.MapWorld;
import xaeroplus.XaeroPlus;
import xaeroplus.settings.XaeroPlusSetting;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.settings.XaeroPlusSettingsReflectionHax;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
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
    public static XaeroPlusSettingRegistry.DataFolderResolutionMode dataFolderResolutionMode = XaeroPlusSettingRegistry.DataFolderResolutionMode.IP;
    public static int minimapScalingFactor = 1;
    public static boolean shouldResetFBO = false;
    public static String LOCK_ID = UUID.randomUUID().toString();
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
    public static String waypointsSearchFilter = "";
    public static List<GuiButton> guiMapButtonTempList = Lists.<GuiButton>newArrayList();
    public static ExecutorService cacheRefreshExecutorService = Executors.newFixedThreadPool(
            // limited benefits by refreshing on more threads as it will consume the entire CPU and start lagging the game
            Math.max(1, Math.min(Runtime.getRuntime().availableProcessors() / 2, 4)));
    public static final ResourceLocation xpGuiTextures = new ResourceLocation("xaeroplus", "gui/xpgui.png");

    public static void onAllSettingsLoaded() {
        XaeroPlusSettingsReflectionHax.ALL_SETTINGS.get().forEach(XaeroPlusSetting::init);
        nullOverworldDimensionFolder = XaeroPlusSettingRegistry.nullOverworldDimensionFolder.getValue();
        dataFolderResolutionMode = XaeroPlusSettingRegistry.dataFolderResolutionMode.getValue();
        minimapScalingFactor = (int) XaeroPlusSettingRegistry.minimapScaling.getValue();
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
