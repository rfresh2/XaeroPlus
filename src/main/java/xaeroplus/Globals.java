package xaeroplus;

import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
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
import xaeroplus.feature.extensions.CustomDimensionMapSaveLoad;
import xaeroplus.feature.render.DrawManager;
import xaeroplus.settings.XaeroPlusSetting;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.settings.XaeroPlusSettingsReflectionHax;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.zip.ZipInputStream;

import static net.minecraft.world.World.OVERWORLD;

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
    public static RegistryKey<World> customDimensionId = OVERWORLD;
    public static String waypointsSearchFilter = "";
    public static List<ButtonWidget> guiMapButtonTempList = Lists.<ButtonWidget>newArrayList();
    public static Supplier<ExecutorService> cacheRefreshExecutorService = Suppliers.memoize(() -> Executors.newFixedThreadPool(
            // limited benefits by refreshing on more threads as it will consume the entire CPU and start lagging the game
            Math.max(1, Math.min(Runtime.getRuntime().availableProcessors() / 2, 4)),
            new ThreadFactoryBuilder()
                .setNameFormat("XaeroPlus-Cache-Refresh-%d")
                .setDaemon(true)
                .build()));

    public static final Identifier xpGuiTextures = new Identifier("xaeroplus", "gui/xpgui.png");

    public static DrawManager drawManager = new DrawManager();

    public static void onAllSettingsDoneLoading() {
        XaeroPlusSettingsReflectionHax.ALL_SETTINGS.get().forEach(XaeroPlusSetting::init);
        nullOverworldDimensionFolder = XaeroPlusSettingRegistry.nullOverworldDimensionFolder.getValue();
        dataFolderResolutionMode = XaeroPlusSettingRegistry.dataFolderResolutionMode.getValue();
        minimapScalingFactor = (int) XaeroPlusSettingRegistry.minimapScaling.getValue();
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
        WorldMap.settings.minimapRadar = worldDim == newDimId;
        customDimensionId = newDimId;
        SupportMods.xaeroMinimap.requestWaypointsRefresh();
    }

    public static byte[] decompressZipToBytes(final Path input) {
        // overall performance improvement here is not that large
        // writing has bigger impact doing it all bytes at once
        try (final var in = new ZipInputStream(Files.newInputStream(input))) {
            if (in.getNextEntry() != null) { // all xaero zip files (currently) only have 1 entry
                return in.readAllBytes();
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        return new byte[0];
    }
}
