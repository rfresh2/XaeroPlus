package xaeroplus.feature.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.lenni0451.lambdaevents.EventHandler;
import xaero.common.minimap.render.MinimapRendererHelper;
import xaeroplus.XaeroPlus;
import xaeroplus.event.DimensionSwitchEvent;
import xaeroplus.event.XaeroWorldChangeEvent;
import xaeroplus.util.ChunkUtils;
import xaeroplus.util.ColorHelper;

import java.util.function.IntSupplier;

public class DrawManager {
    private final Object2ObjectMap<String, DrawFeature> chunkHighlightDrawFeatures = new Object2ObjectOpenHashMap<>();

    public DrawManager() {
        XaeroPlus.EVENT_BUS.register(this);
    }

    public synchronized void registerChunkHighlightProvider(String id, ChunkHighlightSupplier chunkHighlightSupplier, IntSupplier colorSupplier) {
        chunkHighlightDrawFeatures.put(id, new DrawFeature(new ChunkHighlightProvider(chunkHighlightSupplier, colorSupplier)));
    }

    public synchronized void unregisterChunkHighlightProvider(String id) {
        chunkHighlightDrawFeatures.remove(id);
    }

    public synchronized void registerChunkHighlightProvider(Class<?> clazz, ChunkHighlightSupplier chunkHighlightSupplier, IntSupplier colorSupplier) {
        registerChunkHighlightProvider(clazz.getName(), chunkHighlightSupplier, colorSupplier);
    }

    public synchronized void unregisterChunkHighlightProvider(Class<?> clazz) {
        unregisterChunkHighlightProvider(clazz.getName());
    }

    @EventHandler
    public void onDimensionChange(DimensionSwitchEvent event) {
        chunkHighlightDrawFeatures.values().forEach(DrawFeature::invalidateCache);
    }

    @EventHandler
    public void onXaeroWorldChange(XaeroWorldChangeEvent event) {
        chunkHighlightDrawFeatures.values().forEach(DrawFeature::invalidateCache);
    }

    public synchronized void drawMinimapFeatures(
        int minViewMapTileChunkCoordX,
        int maxViewMapTileChunkCoordX,
        int minViewMapTileChunkCoordZ,
        int maxViewMapTileChunkCoordZ,
        int chunkX,
        int chunkZ,
        int tileX,
        int tileZ,
        int insideX,
        int insideZ,
        final PoseStack matrixStack,
        final VertexConsumer overlayBufferBuilder,
        MinimapRendererHelper helper
    ) {
        if (chunkHighlightDrawFeatures.isEmpty()) return;
        for (DrawFeature feature : chunkHighlightDrawFeatures.values()) {
            int color = feature.colorInt();
            var a = ColorHelper.getA(color);
            if (a == 0.0f) return;
            var r = ColorHelper.getR(color);
            var g = ColorHelper.getG(color);
            var b = ColorHelper.getB(color);
            var highlights = feature.getChunkHighlights();
            for (int i = 0; i < highlights.size(); i++) {
                long highlight = highlights.getLong(i);
                var chunkPosX = ChunkUtils.longToChunkX(highlight);
                var chunkPosZ = ChunkUtils.longToChunkZ(highlight);
                var mapTileChunkX = ChunkUtils.chunkCoordToMapTileChunkCoord(chunkPosX);
                var mapTileChunkZ = ChunkUtils.chunkCoordToMapTileChunkCoord(chunkPosZ);
                if (mapTileChunkX < minViewMapTileChunkCoordX || mapTileChunkX > maxViewMapTileChunkCoordX) continue;
                if (mapTileChunkZ < minViewMapTileChunkCoordZ || mapTileChunkZ > maxViewMapTileChunkCoordZ) continue;
                var regionX = ChunkUtils.chunkCoordToRegionCoord(chunkPosX);
                var regionZ = ChunkUtils.chunkCoordToRegionCoord(chunkPosZ);
                var cx = regionX & 7;
                var cz = regionZ & 7;
                var drawX = ((cx - chunkX) << 6) - (tileX << 4) - insideX;
                var drawZ = ((cz - chunkZ) << 6) - (tileZ << 4) - insideZ;
                var left = drawX + 16 * (chunkPosX - cx * 4);
                var top = drawZ + 16 * (chunkPosZ - cz * 4);
                helper.addColoredRectToExistingBuffer(
                    matrixStack.last().pose(), overlayBufferBuilder,
                    left, top, 16, 16,
                    r, g, b, a
                );
            }
        }
    }

    public synchronized void drawWorldMapFeatures(
        final int minMapRegionX,
        final int maxMapRegionX,
        final int minMapRegionZ,
        final int maxMapRegionZ,
        final int flooredCameraX,
        final int flooredCameraZ,
        final PoseStack matrixStack,
        final VertexConsumer overlayBuffer
    ) {
        for (DrawFeature feature : chunkHighlightDrawFeatures.values()) {
            int color = feature.colorInt();
            var a = ColorHelper.getA(color);
            if (a == 0.0f) return;
            var r = ColorHelper.getR(color);
            var g = ColorHelper.getG(color);
            var b = ColorHelper.getB(color);
            var highlights = feature.getChunkHighlights();
            for (int i = 0; i < highlights.size(); i++) {
                long highlight = highlights.getLong(i);
                var chunkPosX = ChunkUtils.longToChunkX(highlight);
                var chunkPosZ = ChunkUtils.longToChunkZ(highlight);
                var regionX = ChunkUtils.chunkCoordToMapRegionCoord(chunkPosX);
                var regionZ = ChunkUtils.chunkCoordToMapRegionCoord(chunkPosZ);
                if (regionX < minMapRegionX || regionX > maxMapRegionX) continue;
                if (regionZ < minMapRegionZ || regionZ > maxMapRegionZ) continue;
                final float left = (float) (ChunkUtils.chunkCoordToCoord(chunkPosX) - flooredCameraX);
                final float top = (float) (ChunkUtils.chunkCoordToCoord(chunkPosZ) - flooredCameraZ);
                final float right = left + 16;
                final float bottom = top + 16;
                MinimapBackgroundDrawHelper.fillIntoExistingBuffer(
                    matrixStack.last().pose(), overlayBuffer,
                    left, top, right, bottom,
                    r, g, b, a
                );
            }
        }
    }
}
