package xaeroplus.fabric.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xaero.common.HudMod;
import xaero.common.XaeroMinimapSession;
import xaero.hud.minimap.common.config.option.MinimapProfiledConfigOptions;
import xaero.map.WorldMapSession;
import xaero.map.gui.GuiMap;
import xaeroplus.feature.render.line.Line;
import xaeroplus.feature.render.text.Text;
import xaeroplus.module.ModuleManager;
import xaeroplus.module.impl.Drawing;
import xaeroplus.settings.Settings;
import xaeroplus.util.ChunkUtils;
import xaeroplus.util.ColorHelper;
import xaeroplus.util.GuiMapHelper;
import java.util.function.Predicate;

public class XaeroPlusClientGameTest implements FabricClientGameTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(XaeroPlusClientGameTest.class);

    @Override
    public void runTest(ClientGameTestContext context) {
        try (var singleplayer = context.worldBuilder().create()) {
            singleplayer.getClientLevel().waitForChunksRender();
            waitFor(context, "client to join world", XaeroPlusClientGameTest::isInWorld);
            waitFor(context, "Xaero world map session", mc -> {
                var session = WorldMapSession.getCurrentSession();
                return session != null && session.getMapProcessor() != null;
            });
            waitFor(context, "Xaero Minimap Session", mc -> {
                var session = XaeroMinimapSession.getCurrentSession();
                return session != null && session.getMinimapProcessor() != null;
            });
            context.runOnClient(mc -> {
                ModuleManager.getModule(Drawing.class).addHighlight(ChunkUtils.actualPlayerChunkX() + 5, ChunkUtils.actualPlayerChunkZ() - 5);
                var lx = ChunkUtils.chunkCoordToCoord(ChunkUtils.actualPlayerChunkX());
                var lz = ChunkUtils.chunkCoordToCoord(ChunkUtils.actualPlayerChunkZ());
                ModuleManager.getModule(Drawing.class).addLine(new Line(lx - 128, lz - 128, lx + 128, lz + 128), ColorHelper.getColor(255, 0, 0, 200));
                ModuleManager.getModule(Drawing.class).addText(new Text("testing the text", lx, lz + 64, ColorHelper.getColor(255, 255, 255, 255), 1f));
                HudMod.INSTANCE.getHudConfigs().getClientConfigManager().getCurrentProfile().set(MinimapProfiledConfigOptions.SIZE, 250);
                HudMod.INSTANCE.getHudConfigs().getClientConfigManager().getCurrentProfile().set(MinimapProfiledConfigOptions.NORTH_LOCKED, true);
            });
            waitForStable(context, "minimap textures", XaeroPlusClientGameTest::isMinimapReady, 20);
            takeScreenshot(context, "world_join");
            context.runOnClient(mc -> {
                Settings.REGISTRY.transparentMinimapBackground.setValue(true);
                Settings.REGISTRY.minimapScaleMultiplierSetting.setValue(2);
                Settings.REGISTRY.minimapFpsLimiter.setValue(true);
            });
            takeScreenshot(context, "minimap_transparent");

            context.runOnClient(mc -> {
                var session = WorldMapSession.getCurrentSession();
                mc.gui.setScreen(new GuiMap(null, null, session.getMapProcessor(), mc.getCameraEntity()));
            });
            waitFor(context, "world map screen", mc -> mc.gui.screen() instanceof GuiMap);
            context.runOnClient(mc -> {
                var guiMap = GuiMapHelper.getGuiMap().get();
                try {
                    var changeZoomMethod = GuiMap.class.getDeclaredMethod("changeZoom", double.class, int.class);
                    changeZoomMethod.setAccessible(true);
                    changeZoomMethod.invoke(guiMap, -5f, 2);
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException("Failed to change world map zoom", e);
                }
            });
            takeScreenshot(context, "world_map");
            context.runOnClient(mc -> {
                Settings.REGISTRY.transparentWorldmapBackgroundSetting.setValue(true);
            });

            takeScreenshot(context, "world_map_transparent");

            LOGGER.info("XaeroPlus client GameTest passed");

            // todo: idk why but mc enters a deadlock if we don't exit the server ourselves
            singleplayer.getServer().runOnServer(server -> server.halt(false));
        }
        // todo: prevent dep threads from causing the shutdown watchdog timeout crash
        System.exit(0);
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
                if (!mc.level.getChunkSource().hasChunk(playerChunk.x() + offsetX, playerChunk.z() + offsetZ)) {
                    return false;
                }
            }
        }

        var minTileChunkX = ChunkUtils.chunkCoordToMapTileChunkCoord(playerChunk.x() - radius);
        var maxTileChunkX = ChunkUtils.chunkCoordToMapTileChunkCoord(playerChunk.x() + radius);
        var minTileChunkZ = ChunkUtils.chunkCoordToMapTileChunkCoord(playerChunk.z() - radius);
        var maxTileChunkZ = ChunkUtils.chunkCoordToMapTileChunkCoord(playerChunk.z() + radius);

        for (var tileChunkX = minTileChunkX; tileChunkX <= maxTileChunkX; tileChunkX++) {
            for (var tileChunkZ = minTileChunkZ; tileChunkZ <= maxTileChunkZ; tileChunkZ++) {
                if (!tileIntersectsRenderDistance(tileChunkX, tileChunkZ, playerChunk.x(), playerChunk.z(), radius)) {
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
                if (chunk.getToUpdateBuffers() || !texture.isUploaded() || texture.getGlColorTexture() == null) {
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

    private static void waitFor(ClientGameTestContext context, String description, Predicate<Minecraft> condition) {
        LOGGER.info("Waiting for {}", description);
        context.waitFor(condition, (20 * 60));
    }

    private static void waitForStable(ClientGameTestContext context, String description, Predicate<Minecraft> condition, int stableTicks) {
        LOGGER.info("Waiting for {} to be stable for {} ticks", description, stableTicks);
        var consecutiveTicks = 0;
        for (var elapsedTicks = 0; consecutiveTicks < stableTicks; elapsedTicks++) {
            if (elapsedTicks >= (20 * 60)) {
                throw new IllegalStateException("Timed out waiting for " + description);
            }
            consecutiveTicks = context.computeOnClient(condition::test) ? consecutiveTicks + 1 : 0;
            context.waitTick();
        }
    }

    private static void takeScreenshot(ClientGameTestContext context, String name) {
        context.waitTicks(20);
        context.takeScreenshot(name);
    }
}
