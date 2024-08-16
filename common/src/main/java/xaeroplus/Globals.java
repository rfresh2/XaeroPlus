package xaeroplus;

import com.google.common.base.Suppliers;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaero.hud.HudSession;
import xaero.map.core.XaeroWorldMapCore;
import xaeroplus.event.ClientPlaySessionFinalizedEvent;
import xaeroplus.feature.render.DrawManager;
import xaeroplus.settings.XaeroPlusSetting;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.settings.XaeroPlusSettingsReflectionHax;

import java.io.ByteArrayOutputStream;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import static net.minecraft.world.level.Level.OVERWORLD;

/**
 * static variables and functions to share or persist across mixins
 */
public class Globals {
    public static DrawManager drawManager = new DrawManager();
    // Map gui follow mode
    public static boolean FOLLOW = false;
    // cache and only update this on new world loads
    public static boolean nullOverworldDimensionFolder = false;
    public static XaeroPlusSettingRegistry.DataFolderResolutionMode dataFolderResolutionMode = XaeroPlusSettingRegistry.DataFolderResolutionMode.IP;
    public static int minimapScaleMultiplier = 1;
    public static int minimapSizeMultiplier = 1;
    public static boolean shouldResetFBO = false;
    public static String LOCK_ID = UUID.randomUUID().toString();
    public static PoseStack minimapDrawContext = null;
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
    public static Supplier<ExecutorService> cacheRefreshExecutorService = Suppliers.memoize(() -> Executors.newFixedThreadPool(
            // limited benefits by refreshing on more threads as it will consume the entire CPU and start lagging the game
            Math.max(1, Math.min(Runtime.getRuntime().availableProcessors() / 2, 4)),
            new ThreadFactoryBuilder()
                .setNameFormat("XaeroPlus-Cache-Refresh-%d")
                .setDaemon(true)
                .build()));

    public static void onAllSettingsLoaded() {
        XaeroPlusSettingsReflectionHax.ALL_SETTINGS.get().forEach(XaeroPlusSetting::init);
        nullOverworldDimensionFolder = XaeroPlusSettingRegistry.nullOverworldDimensionFolder.getValue();
        dataFolderResolutionMode = XaeroPlusSettingRegistry.dataFolderResolutionMode.getValue();
        minimapScaleMultiplier = (int) XaeroPlusSettingRegistry.minimapScaleMultiplierSetting.getValue();
        minimapSizeMultiplier = (int) XaeroPlusSettingRegistry.minimapSizeMultiplierSetting.getValue();
        XaeroPlus.EVENT_BUS.registerConsumer((e) -> {
            nullOverworldDimensionFolder = XaeroPlusSettingRegistry.nullOverworldDimensionFolder.getValue();
            dataFolderResolutionMode = XaeroPlusSettingRegistry.dataFolderResolutionMode.getValue();
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

    public static void setDataFolderResolutionModeIfAble(XaeroPlusSettingRegistry.DataFolderResolutionMode mode) {
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
