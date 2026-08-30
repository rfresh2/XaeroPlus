package xaeroplus.fabric.gametest;

import it.unimi.dsi.fastutil.longs.Long2LongArrayMap;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xaero.common.HudMod;
import xaero.common.XaeroMinimapSession;
import xaero.common.gui.GuiMinimapMain;
import xaero.common.gui.GuiWaypoints;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.common.config.option.MinimapProfiledConfigOptions;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.lib.client.gui.config.context.BuiltInEditConfigScreenContexts;
import xaero.map.WorldMapSession;
import xaero.map.gui.GuiMap;
import xaero.map.gui.GuiWorldMapSettings;
import xaeroplus.Globals;
import xaeroplus.feature.drawing.ColorPickerWidget;
import xaeroplus.feature.drawing.DrawingColorPickerButton;
import xaeroplus.feature.extensions.DrawOrderScreen;
import xaeroplus.feature.extensions.GuiMinimapWaypointTeleportCommandSettings;
import xaeroplus.feature.extensions.SyncedWaypoint;
import xaeroplus.feature.render.DrawFeatureFactory;
import xaeroplus.feature.render.line.Line;
import xaeroplus.feature.render.text.Text;
import xaeroplus.feature.waypoint.WaypointAPI;
import xaeroplus.module.ModuleManager;
import xaeroplus.module.impl.Drawing;
import xaeroplus.settings.Settings;
import xaeroplus.util.ChunkUtils;
import xaeroplus.util.ColorHelper;
import xaeroplus.util.GuiMapHelper;
import xaeroplus.util.Wait;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Predicate;

public class XaeroPlusClientGameTest implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(XaeroPlusClientGameTest.class);
    private static final AtomicLong CLIENT_TICKS = new AtomicLong();

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> CLIENT_TICKS.incrementAndGet());
        var testThread = new Thread(this::runTest, "XaeroPlus Client GameTest");
        testThread.setDaemon(true);
        testThread.start();
    }

    private void runTest() {
        try {
            waitFor("initial resource load", mc -> mc.getOverlay() == null);
            submit(mc -> {
                Minecraft.getInstance().options.tutorialStep = TutorialSteps.NONE;
                return null;
            });
            if (submit(mc -> mc.screen instanceof AccessibilityOnboardingScreen)) {
                clickButton("gui.continue");
            }
            waitFor("title screen", mc -> mc.getOverlay() == null && mc.screen instanceof TitleScreen);
            clickButton("menu.singleplayer");
            waitFor("world selection", mc -> mc.screen instanceof SelectWorldScreen || mc.screen instanceof CreateWorldScreen);
            if (submit(mc -> mc.screen instanceof SelectWorldScreen)) {
                clickButton("selectWorld.create");
                waitFor("create world screen", mc -> mc.screen instanceof CreateWorldScreen);
            }
            clickButton("selectWorld.create");

            waitFor("world load confirmation or joined world", mc -> mc.screen instanceof ConfirmScreen || isInWorld(mc));
            if (submit(mc -> mc.screen instanceof ConfirmScreen)) {
                clickButton("gui.yes");
            }

            waitFor("client to join world", XaeroPlusClientGameTest::isInWorld);
            waitFor("Xaero world map session", mc -> {
                var session = WorldMapSession.getCurrentSession();
                return session != null && session.getMapProcessor() != null;
            });
            waitFor("Xaero Minimap Session", mc -> {
                var session = XaeroMinimapSession.getCurrentSession();
                return session != null && session.getMinimapProcessor() != null && WaypointAPI.getCurrentWaypointSet() != null;
            });
            submit(mc -> {
                ModuleManager.getModule(Drawing.class).addHighlight(ChunkUtils.actualPlayerChunkX() + 5, ChunkUtils.actualPlayerChunkZ() - 5);
                var lx = ChunkUtils.chunkCoordToCoord(ChunkUtils.actualPlayerChunkX());
                var lz = ChunkUtils.chunkCoordToCoord(ChunkUtils.actualPlayerChunkZ());
                ModuleManager.getModule(Drawing.class).addLine(new Line(lx - 128, lz - 128, lx + 128, lz + 128), ColorHelper.getColor(255, 0, 0, 200));
                ModuleManager.getModule(Drawing.class).addText(new Text("bottom text", lx, lz - 96, ColorHelper.getColor(255, 255, 255, 255), 1f));
                var drawing = ModuleManager.getModule(Drawing.class);
                drawing.addEllipse(
                    drawing.snapEllipse(lx + 52, lz, lx + 80, lz + 33, 1.0),
                    ColorHelper.getColor(0, 255, 255, 230)
                );
                drawing.addEllipse(
                    drawing.ellipseFromCenterAndRadii(lx - 52, lz, lx - 12, lz - 20),
                    ColorHelper.getColor(255, 128, 0, 230)
                );
                var testHighlights = new Long2LongArrayMap();
                testHighlights.put(ChunkUtils.chunkPosToLong(ChunkUtils.actualPlayerChunkX() - 5, ChunkUtils.actualPlayerChunkZ() + 5), 0);
                Globals.drawManager.registry().register(DrawFeatureFactory.chunkHighlights(
                    "test",
                    dim -> testHighlights,
                    () -> ColorHelper.getColor(0, 255, 0, 150),
                    50
                ));
                HudMod.INSTANCE.getHudConfigs().getClientConfigManager().getCurrentProfile().set(MinimapProfiledConfigOptions.SIZE, 200);
                HudMod.INSTANCE.getHudConfigs().getClientConfigManager().getCurrentProfile().set(MinimapProfiledConfigOptions.NORTH_LOCKED, true);
                HudMod.INSTANCE.getHudConfigs().getClientConfigManager().getCurrentProfile().set(MinimapProfiledConfigOptions.WAYPOINT_DISTANCE_IN_WORLD, 2);
                WaypointAPI.getCurrentWaypointSet().add(SyncedWaypoint.create((int) ChunkUtils.getPlayerX() - 64, (int) ChunkUtils.getPlayerZ() + 64, "Test", "long initials", WaypointColor.AQUA));
                Settings.REGISTRY.waypointBeacons.setValue(true);
                Settings.REGISTRY.waypointEta.setValue(true);
                Settings.REGISTRY.showRenderDistanceSetting.setValue(true);
                Settings.REGISTRY.longWaypointInitials.setValue(true);
                return null;
            });
            waitForStable("minimap textures", XaeroPlusClientGameTest::isMinimapReady, 20);
            takeScreenshot("world_join");
            submit(mc -> {
                HudMod.INSTANCE.getHudConfigs().getClientConfigManager().getCurrentProfile().set(MinimapProfiledConfigOptions.SIZE, 100);
                Settings.REGISTRY.minimapScaleMultiplierSetting.setValue(2);
                Settings.REGISTRY.minimapSizeMultiplierSetting.setValue(2);
                Settings.REGISTRY.minimapFpsLimiter.setValue(true);
                return null;
            });
            takeScreenshot("minimap_resized");

            submit(mc -> {
                var session = WorldMapSession.getCurrentSession();
                mc.setScreen(new GuiMap(null, null, session.getMapProcessor(), mc.getCameraEntity()));
                return null;
            });
            waitFor("world map screen", mc -> mc.screen instanceof GuiMap);
            submit(mc -> {
                var guiMap = GuiMapHelper.getGuiMap().get();
                try {
                    var changeZoomMethod = GuiMap.class.getDeclaredMethod("changeZoom", double.class, int.class);
                    changeZoomMethod.setAccessible(true);
                    changeZoomMethod.invoke(guiMap, -5f, 2);
                } catch (Exception e) {
                    System.exit(1);
                    throw new RuntimeException(e);
                }
                return null;
            });
            takeScreenshot("world_map");

            clickButtonContaining("xaeroplus.gui.world_map.start_drawing");
            clickButton(DrawingColorPickerButton.class);
            waitFor("drawing color picker", mc -> mc.screen != null && mc.screen.children().stream()
                .anyMatch(child -> child instanceof ColorPickerWidget colorPicker && colorPicker.visible));
            takeScreenshot("world_map_drawing_color_picker");
            clickButtonContaining("xaeroplus.gui.world_map.start_drawing");

            submit(mc -> {
                Settings.REGISTRY.transparentWorldmapBackgroundSetting.setValue(true);
                return null;
            });

            takeScreenshot("world_map_transparent");

            submit(mc -> {
                mc.setScreen(new GuiWorldMapSettings(mc.screen, null, BuiltInEditConfigScreenContexts.CLIENT));
                return null;
            });
            takeScreenshot("world_map_settings");

            submit(mc -> {
                mc.setScreen(new GuiMinimapMain(XaeroMinimapSession.getCurrentSession().getModMain(), mc.screen, null, true, BuiltInEditConfigScreenContexts.CLIENT));
                return null;
            });
            takeScreenshot("minimap_settings");

            submit(mc -> {
                Settings.REGISTRY.waypointsListDistanceColumn.setValue(true);
                mc.setScreen(new GuiWaypoints(HudMod.INSTANCE, BuiltInHudModules.MINIMAP.getCurrentSession(), null, null));
                return null;
            });
            takeScreenshot("waypoints_list");

            submit(mc -> {
                mc.setScreen(new DrawOrderScreen(null, null));
                return null;
            });
            takeScreenshot("draw_order_screen");

            submit(mc -> {
                mc.setScreen(new GuiMinimapWaypointTeleportCommandSettings(null, null, Settings.REGISTRY.crossDimensionWaypointTeleportFormat));
                return null;
            });
            takeScreenshot("minimap_waypoint_teleport_screen");

            LOGGER.info("XaeroPlus client GameTest passed");
            submit(mc -> {
                mc.stop();
                return null;
            });
        } catch (Throwable throwable) {
            LOGGER.error("XaeroPlus client GameTest failed", throwable);
            System.exit(1);
        }
    }

    private static boolean isInWorld(Minecraft mc) {
        return mc.level != null && mc.player != null && mc.getCameraEntity() != null;
    }

    private static boolean isMinimapReady(Minecraft mc) {
        var session = WorldMapSession.getCurrentSession();
        if (session == null) {
            return false;
        }

        var processor = session.getMapProcessor();
        if (processor == null || !processor.isMapWorldUsable()) {
            return false;
        }
        if (!processor.getMapSaveLoad().isRegionDetectionComplete()) {
            return false;
        }

        var playerChunk = mc.player.chunkPosition();
        var radius = mc.options.renderDistance().get();

        for (var offsetX = -radius; offsetX <= radius; offsetX++) {
            for (var offsetZ = -radius; offsetZ <= radius; offsetZ++) {
                if (!isWithinRenderDistance(offsetX, offsetZ, radius)) {
                    continue;
                }
                if (!mc.level.getChunkSource().hasChunk(playerChunk.x + offsetX, playerChunk.z + offsetZ)) {
                    return false;
                }
            }
        }

        var minTileChunkX = ChunkUtils.chunkCoordToMapTileChunkCoord(playerChunk.x - radius);
        var maxTileChunkX = ChunkUtils.chunkCoordToMapTileChunkCoord(playerChunk.x + radius);
        var minTileChunkZ = ChunkUtils.chunkCoordToMapTileChunkCoord(playerChunk.z - radius);
        var maxTileChunkZ = ChunkUtils.chunkCoordToMapTileChunkCoord(playerChunk.z + radius);

        for (var tileChunkX = minTileChunkX; tileChunkX <= maxTileChunkX; tileChunkX++) {
            for (var tileChunkZ = minTileChunkZ; tileChunkZ <= maxTileChunkZ; tileChunkZ++) {
                if (!tileIntersectsRenderDistance(tileChunkX, tileChunkZ, playerChunk.x, playerChunk.z, radius)) {
                    continue;
                }

                var regionX = ChunkUtils.mapTileChunkCoordToMapRegionCoord(tileChunkX);
                var regionZ = ChunkUtils.mapTileChunkCoordToMapRegionCoord(tileChunkZ);
                var region = processor.getLeafMapRegion(processor.getCurrentCaveLayer(), regionX, regionZ, false);
                if (region == null || !region.isLoaded()) {
                    return false;
                }

                var chunk = region.getChunk(Math.floorMod(tileChunkX, 8), Math.floorMod(tileChunkZ, 8));
                if (chunk == null) {
                    return false;
                }

                var texture = chunk.getLeafTexture();
                if (chunk.getToUpdateBuffers() || !texture.isUploaded() || texture.getGlColorTexture() == -1) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean tileIntersectsRenderDistance(int tileChunkX, int tileChunkZ, int centerChunkX, int centerChunkZ, int radius) {
        for (var chunkX = ChunkUtils.mapTileChunkCoordToChunkCoord(tileChunkX); chunkX < ChunkUtils.mapTileChunkCoordToChunkCoord(tileChunkX + 1); chunkX++) {
            for (var chunkZ = ChunkUtils.mapTileChunkCoordToChunkCoord(tileChunkZ); chunkZ < ChunkUtils.mapTileChunkCoordToChunkCoord(tileChunkZ + 1); chunkZ++) {
                if (isWithinRenderDistance(chunkX - centerChunkX, chunkZ - centerChunkZ, radius)) return true;
            }
        }
        return false;
    }

    private static boolean isWithinRenderDistance(int offsetX, int offsetZ, int radius) {
        return offsetX * offsetX + offsetZ * offsetZ <= radius * radius;
    }

    private static void clickButton(String translationKey) {
        var expectedLabel = Component.translatable(translationKey).getString();
        waitFor("button " + expectedLabel, mc -> {
            var screen = mc.screen;
            if (screen == null) return false;

            for (var child : screen.children()) {
                if (child instanceof Button button && expectedLabel.equals(button.getMessage().getString())) {
                    button.onPress();
                    return true;
                }
            }
            return false;
        });
    }

    private static void clickButtonContaining(String translationKey) {
        var expectedLabel = Component.translatable(translationKey).getString();
        waitFor("button containing " + expectedLabel, mc -> {
            var screen = mc.screen;
            if (screen == null) return false;

            for (var child : screen.children()) {
                if (child instanceof Button button && button.getMessage().getString().contains(expectedLabel)) {
                    button.onPress();
                    return true;
                }
            }
            return false;
        });
    }

    private static <T extends Button> void clickButton(Class<T> buttonClass) {
        waitFor("button " + buttonClass.getSimpleName(), mc -> {
            var screen = mc.screen;
            if (screen == null) return false;

            for (var child : screen.children()) {
                if (buttonClass.isInstance(child)) {
                    buttonClass.cast(child).onPress();
                    return true;
                }
            }
            return false;
        });
    }

    private static void waitFor(String description, Predicate<Minecraft> condition) {
        var deadline = Instant.now().plus(Duration.ofMinutes(10));
        String lastScreen = null;
        while (!submit(condition::test)) {
            var screen = submit(mc -> mc.screen == null ? "<none>" : mc.screen.getClass().getName());
            if (!screen.equals(lastScreen)) {
                LOGGER.info("Waiting for {} (current screen: {})", description, screen);
                lastScreen = screen;
            }
            if (Instant.now().isAfter(deadline)) {
                throw new IllegalStateException("Timed out waiting for " + description);
            }
            Wait.waitMs(100);
        }
    }

    private static void waitForStable(String description, Predicate<Minecraft> condition, int stableTicks) {
        var consecutiveTicks = 0;
        var lastTick = -1L;
        var deadline = Instant.now().plus(Duration.ofMinutes(10));
        while (consecutiveTicks < stableTicks) {
            var result = submit(mc -> new StableWaitSample(CLIENT_TICKS.get(), condition.test(mc)));
            if (result.tick() != lastTick) {
                consecutiveTicks = result.ready() ? consecutiveTicks + 1 : 0;
                lastTick = result.tick();
            }
            if (Instant.now().isAfter(deadline)) {
                throw new IllegalStateException("Timed out waiting for " + description);
            }
            waitFor(Duration.ofMillis(10));
        }
    }

    private record StableWaitSample(long tick, boolean ready) {}

    private static void waitFor(Duration duration) {
        Wait.waitMs((int) duration.toMillis());
    }

    private static <T> T submit(Function<Minecraft, T> action) {
        var mc = Minecraft.getInstance();
        return mc.submit(() -> action.apply(mc)).join();
    }

    public static void takeScreenshot(String name) {
        // Allow time for any screens to open
        waitFor(Duration.ofSeconds(1));

        submit(mc -> {
            Screenshot.grab(FabricLoader.getInstance().getGameDir().toFile(), name + ".png", mc.getMainRenderTarget(), (message) -> {});
            return null;
        });
    }
}
