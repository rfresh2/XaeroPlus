package xaeroplus;

import com.google.common.base.Suppliers;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaero.hud.HudSession;
import xaero.map.core.XaeroWorldMapCore;
import xaeroplus.event.ClientPlaySessionFinalizedEvent;
import xaeroplus.feature.render.DrawManager;
import xaeroplus.settings.Settings;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import static net.minecraft.world.level.Level.OVERWORLD;

/**
 * static variables and functions to share or persist across mixins
 */
public class Globals {
    public static final DrawManager drawManager = new DrawManager();
    // cache and only update this on new world loads
    public static boolean nullOverworldDimensionFolder = false;
    public static Settings.DataFolderResolutionMode dataFolderResolutionMode = Settings.DataFolderResolutionMode.IP;
    public static int minimapScaleMultiplier = 1;
    public static int minimapSizeMultiplier = 1;
    public static boolean shouldResetFBO = false;
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
    // This can only be shared under the assumption region and texture cache writes are non-concurrent
    // sharing the underlying byte array reduces GC spam
    // at cost of a few MB higher idle RAM usage
    public static ByteArrayOutputStream zipFastByteBuffer = new ByteArrayOutputStream();
    public static final Supplier<ExecutorService> cacheRefreshExecutorService = Suppliers.memoize(() -> Executors.newFixedThreadPool(
            // limited benefits by refreshing on more threads as it will consume the entire CPU and start lagging the game
            Math.max(1, Math.min(Runtime.getRuntime().availableProcessors() / 2, 2)),
            new ThreadFactoryBuilder()
                .setNameFormat("XaeroPlus-Cache-Refresh-%d")
                .setUncaughtExceptionHandler((t, e) -> XaeroPlus.LOGGER.error("Caught unhandled exception in cache refresh executor", e))
                .setDaemon(true)
                .build()));
    public static final Supplier<ExecutorService> moduleExecutorService = Suppliers.memoize(() -> Executors.newFixedThreadPool(
        Math.max(1, Math.min(Runtime.getRuntime().availableProcessors() / 2, 2)),
        new ThreadFactoryBuilder()
            .setNameFormat("XaeroPlus-Module-%d")
            .setUncaughtExceptionHandler((t, e) -> XaeroPlus.LOGGER.error("Caught unhandled exception in module executor", e))
            .setDaemon(true)
            .build()));

    public static void initStickySettings() {
        nullOverworldDimensionFolder = Settings.REGISTRY.nullOverworldDimensionFolder.get();
        dataFolderResolutionMode = Settings.REGISTRY.dataFolderResolutionMode.get();
        minimapScaleMultiplier = Settings.REGISTRY.minimapScaleMultiplierSetting.getAsInt();
        minimapSizeMultiplier = Settings.REGISTRY.minimapSizeMultiplierSetting.getAsInt();
        XaeroPlus.EVENT_BUS.registerConsumer((e) -> {
            nullOverworldDimensionFolder = Settings.REGISTRY.nullOverworldDimensionFolder.get();
            dataFolderResolutionMode = Settings.REGISTRY.dataFolderResolutionMode.get();
        }, ClientPlaySessionFinalizedEvent.class);
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

    public static void setNullOverworldDimFolderIfAble(final boolean b) {
        try {
            var currentWMSession = XaeroWorldMapCore.currentSession;
            var currentMMSession = HudSession.getCurrentSession();
            if (currentWMSession != null || currentMMSession != null) return;
            Globals.nullOverworldDimensionFolder = b;
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Failed setting nullOverworldDimensionFolder", e);
        }
    }

    public static void setDataFolderResolutionModeIfAble(Settings.DataFolderResolutionMode mode) {
        try {
            var currentWMSession = XaeroWorldMapCore.currentSession;
            var currentMMSession = HudSession.getCurrentSession();
            if (currentWMSession != null || currentMMSession != null) return;
            Globals.dataFolderResolutionMode = mode;
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Failed setting data folder resolution mode", e);
        }
    }
}
