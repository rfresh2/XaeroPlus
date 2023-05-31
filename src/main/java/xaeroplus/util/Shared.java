package xaeroplus.util;

import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import xaero.map.MapProcessor;
import xaero.map.WorldMap;
import xaero.map.WorldMapSession;
import xaero.map.mods.SupportMods;
import xaero.map.world.MapDimension;
import xaeroplus.settings.XaeroPlusSettingRegistry;

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
public class Shared {

    // Map gui follow mode
    public static boolean FOLLOW = false;
    // cache and only update this on new world loads
    public static boolean nullOverworldDimensionFolder = XaeroPlusSettingRegistry.nullOverworldDimensionFolder.getValue();
    public static XaeroPlusSettingRegistry.DataFolderResolutionMode dataFolderResolutionMode = XaeroPlusSettingRegistry.dataFolderResolutionMode.getValue();
    public static int minimapScalingFactor = (int) XaeroPlusSettingRegistry.minimapScaling.getValue();
    public static boolean settingsLoadedInit = false;
    public static boolean shouldResetFBO = false;
    public static String LOCK_ID = UUID.randomUUID().toString();
    public static int customDimensionId = 0;
    public static String waypointsSearchFilter = "";
    public static List<GuiButton> guiMapButtonTempList = Lists.<GuiButton>newArrayList();
    public static ExecutorService cacheRefreshExecutorService = Executors.newFixedThreadPool(
            // limited benefits by refreshing on more threads as it will consume the entire CPU and start lagging the game
            Math.max(1, Math.min(Runtime.getRuntime().availableProcessors() / 2, 4)));

    public static void onSettingLoad() {
        if (!settingsLoadedInit) { // handle settings where we want them to take effect only on first load
            nullOverworldDimensionFolder = XaeroPlusSettingRegistry.nullOverworldDimensionFolder.getValue();
            dataFolderResolutionMode = XaeroPlusSettingRegistry.dataFolderResolutionMode.getValue();
            minimapScalingFactor = (int) XaeroPlusSettingRegistry.minimapScaling.getValue();
        }
    }

    public static void switchToDimension(final int newDimId) {
        MapProcessor mapProcessor = WorldMapSession.getCurrentSession().getMapProcessor();
        mapProcessor.getMapSaveLoad().setRegionDetectionComplete(false);
        MapDimension dimension = mapProcessor.getMapWorld().getDimension(newDimId);
        if (dimension == null) {
            dimension = mapProcessor.getMapWorld().createDimensionUnsynced(mapProcessor.mainWorld, newDimId);
        }
        if (!dimension.hasDoneRegionDetection()) {
            ((CustomDimensionMapSaveLoad) mapProcessor.getMapSaveLoad()).detectRegionsInDimension(10, newDimId);
        }
        mapProcessor.getMapSaveLoad().setRegionDetectionComplete(true);
        // kind of shit but its ok. need to reset setting when GuiMap closes
        int worldDim = Minecraft.getMinecraft().world.provider.getDimension();
        if (worldDim != newDimId) {
            WorldMap.settings.minimapRadar = false;
        } else {
            WorldMap.settings.minimapRadar = true;
        }
        customDimensionId = newDimId;
        SupportMods.xaeroMinimap.requestWaypointsRefresh();
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
