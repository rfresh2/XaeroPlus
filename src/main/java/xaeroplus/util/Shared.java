package xaeroplus.util;

import com.google.common.collect.Lists;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import xaero.map.MapProcessor;
import xaero.map.WorldMap;
import xaero.map.WorldMapSession;
import xaero.map.mods.SupportMods;
import xaero.map.world.MapDimension;
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

import static net.minecraft.world.World.OVERWORLD;

/**
 * static variables and functions to share or persist across mixins
 */
public class Shared {

    // Map gui follow mode
    public static boolean FOLLOW = false;
    // cache and only update this on new world loads
    public static boolean nullOverworldDimensionFolder = false;
    public static XaeroPlusSettingRegistry.DataFolderResolutionMode dataFolderResolutionMode = XaeroPlusSettingRegistry.DataFolderResolutionMode.IP;
    public static int minimapScalingFactor = 1;
    public static boolean settingsLoadedInit = false;
    public static boolean shouldResetFBO = false;
    public static String LOCK_ID = UUID.randomUUID().toString();
    public static RegistryKey<World> customDimensionId = OVERWORLD;
    public static String waypointsSearchFilter = "";
    public static List<ButtonWidget> guiMapButtonTempList = Lists.<ButtonWidget>newArrayList();
    public static ExecutorService cacheRefreshExecutorService = Executors.newFixedThreadPool(
            // limited benefits by refreshing on more threads as it will consume the entire CPU and start lagging the game
            Math.max(1, Math.min(Runtime.getRuntime().availableProcessors() / 2, 4)));
    public static final Identifier xpGuiTextures = new Identifier("xaeroplus", "gui/xpgui.png");

    public static void onSettingLoad() {
        if (!settingsLoadedInit) { // handle settings where we want them to take effect only on first load
            XaeroPlusSettingRegistry.fastMapSetting.getValue(); // force load all settings if they haven't been already
            XaeroPlusSettingsReflectionHax.ALL_SETTINGS.get().forEach(XaeroPlusSetting::init);
            nullOverworldDimensionFolder = XaeroPlusSettingRegistry.nullOverworldDimensionFolder.getValue();
            dataFolderResolutionMode = XaeroPlusSettingRegistry.dataFolderResolutionMode.getValue();
            minimapScalingFactor = (int) XaeroPlusSettingRegistry.minimapScaling.getValue();
        }
    }

    public static void switchToDimension(final RegistryKey<World> newDimId) {
        WorldMapSession worldMapSession = WorldMapSession.getCurrentSession();
        if (worldMapSession == null) return;
        MapProcessor mapProcessor = worldMapSession.getMapProcessor();
        mapProcessor.getMapSaveLoad().setRegionDetectionComplete(false);
        MapDimension dimension = mapProcessor.getMapWorld().getDimension(newDimId);
        if (dimension == null) {
            dimension = mapProcessor.getMapWorld().createDimensionUnsynced(newDimId);
        }
        if (!dimension.hasDoneRegionDetection()) {
            ((CustomDimensionMapSaveLoad) mapProcessor.getMapSaveLoad()).detectRegionsInDimension(10, newDimId);
        }
        mapProcessor.getMapSaveLoad().setRegionDetectionComplete(true);
        // kind of shit but its ok. need to reset setting when GuiMap closes
        RegistryKey<World> worldDim = MinecraftClient.getInstance().world.getRegistryKey();
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
