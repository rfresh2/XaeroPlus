package xaeroplus;

import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import xaero.map.core.XaeroWorldMapCore;
import xaeroplus.feature.render.DrawManager;
import xaeroplus.settings.XaeroPlusSetting;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.settings.XaeroPlusSettingsReflectionHax;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.zip.ZipInputStream;

import static net.minecraft.world.level.Level.OVERWORLD;

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
    public static GuiGraphics minimapDrawContext = null;
    public static ResourceKey<Level> getCurrentDimensionId() {
        try {
            var dim = XaeroWorldMapCore.currentSession.getMapProcessor().getMapWorld().getCurrentDimensionId();
            if (dim == null) return OVERWORLD;
            else return dim;
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Failed getting current dimension id", e);
            return OVERWORLD;
        }
    }
    public static String waypointsSearchFilter = "";
    public static List<Button> guiMapButtonTempList = Lists.<Button>newArrayList();
    // This can only be shared under the assumption region and texture cache writes are non-concurrent
    // sharing the underlying byte array reduces GC spam
    // at cost of a few MB higher idle RAM usage
    public static ByteArrayOutputStream zipFastByteBuffer = new ByteArrayOutputStream();
    public static Supplier<ExecutorService> cacheRefreshExecutorService = Suppliers.memoize(() -> Executors.newFixedThreadPool(
            // limited benefits by refreshing on more threads as it will consume the entire CPU and start lagging the game
            Math.max(1, Math.min(Runtime.getRuntime().availableProcessors() / 2, 4)),
            new ThreadFactoryBuilder()
                .setNameFormat("XaeroPlus-Cache-Refresh-%d")
                .setDaemon(true)
                .build()));

    public static final ResourceLocation xpGuiTextures = new ResourceLocation("xaeroplus", "gui/xpgui.png");

    public static DrawManager drawManager = new DrawManager();

    public static void onAllSettingsLoaded() {
        XaeroPlusSettingsReflectionHax.ALL_SETTINGS.get().forEach(XaeroPlusSetting::init);
        nullOverworldDimensionFolder = XaeroPlusSettingRegistry.nullOverworldDimensionFolder.getValue();
        dataFolderResolutionMode = XaeroPlusSettingRegistry.dataFolderResolutionMode.getValue();
        minimapScalingFactor = (int) XaeroPlusSettingRegistry.minimapScaling.getValue();
    }

    public static void switchToDimension(final ResourceKey<Level> newDimId) {
        if (newDimId == null) return;
        try {
            var session = XaeroWorldMapCore.currentSession;
            if (session == null) return;
            var mapProcessor = session.getMapProcessor();
            if (mapProcessor == null) return;
            var mapWorld = mapProcessor.getMapWorld();
            if (mapWorld == null) return;
            mapWorld.setCustomDimensionId(newDimId);
            mapProcessor.checkForWorldUpdate();
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Failed switching to dimension: {}", newDimId, e);
        }
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
