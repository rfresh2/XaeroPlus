package xaeroplus.feature.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.lenni0451.lambdaevents.EventHandler;
import net.minecraft.client.Minecraft;
import xaero.common.minimap.render.MinimapRendererHelper;
import xaeroplus.XaeroPlus;
import xaeroplus.event.DimensionSwitchEvent;
import xaeroplus.event.XaeroWorldChangeEvent;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.util.ChunkUtils;
import xaeroplus.util.ColorHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.IntSupplier;

public class DrawManager {
    private final Object2ObjectMap<String, DrawFeature> chunkHighlightDrawFeatures = new Object2ObjectOpenHashMap<>();
    private final List<String> sortedKeySet = new ArrayList<>();

    public DrawManager() {
        XaeroPlus.EVENT_BUS.register(this);
    }

    public synchronized void registerChunkHighlightProvider(String id, ChunkHighlightSupplier chunkHighlightSupplier, IntSupplier colorSupplier) {
        unregisterChunkHighlightProvider(id); // just in case
        chunkHighlightDrawFeatures.put(id, new DrawFeature(new ChunkHighlightProvider(chunkHighlightSupplier, colorSupplier)));
        sortedKeySet.add(id);
        // arbitrary order, just needs to be consistent so colors blend consistently
        sortedKeySet.sort(Comparator.naturalOrder());
    }

    public synchronized void unregisterChunkHighlightProvider(String id) {
        sortedKeySet.remove(id);
        DrawFeature feature = chunkHighlightDrawFeatures.remove(id);
        if (feature != null) {
            Minecraft.getInstance().execute(() -> feature.getHighlightDrawBuffer().close());
        }
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
        matrixStack.pushPose();
        matrixStack.translate(
            -(chunkX * 64) - (tileX * 16) - insideX,
            -(chunkZ * 64) - (tileZ * 16) - insideZ,
            0);
        matrixStack.scale(16f, 16f, 1f);
        if (XaeroPlusSettingRegistry.highlightShader.getValue()) {
            drawMinimapFeaturesShader(matrixStack);
        } else {
            drawMinimapFeaturesImmediate(minViewMapTileChunkCoordX, maxViewMapTileChunkCoordX, minViewMapTileChunkCoordZ, maxViewMapTileChunkCoordZ,
                                         matrixStack, overlayBufferBuilder, helper);
        }
        matrixStack.popPose();
    }

    public synchronized void drawMinimapFeaturesImmediate(
        int minViewMapTileChunkCoordX,
        int maxViewMapTileChunkCoordX,
        int minViewMapTileChunkCoordZ,
        int maxViewMapTileChunkCoordZ,
        final PoseStack matrixStack,
        final VertexConsumer overlayBufferBuilder,
        MinimapRendererHelper helper
    ) {
        var matrix = matrixStack.last().pose();
        for (int i = 0; i < sortedKeySet.size(); i++) {
            var k = sortedKeySet.get(i);
            if (k == null) continue;
            var feature = chunkHighlightDrawFeatures.get(k);
            if (feature == null) continue;
            int color = feature.colorInt();
            var a = ColorHelper.getA(color);
            if (a == 0.0f) return;
            var r = ColorHelper.getR(color);
            var g = ColorHelper.getG(color);
            var b = ColorHelper.getB(color);
            var highlights = feature.getChunkHighlights();
            for (int j = 0; j < highlights.size(); j++) {
                long highlight = highlights.getLong(j);
                var chunkPosX = ChunkUtils.longToChunkX(highlight);
                var chunkPosZ = ChunkUtils.longToChunkZ(highlight);
                var mapTileChunkX = ChunkUtils.chunkCoordToMapTileChunkCoord(chunkPosX);
                var mapTileChunkZ = ChunkUtils.chunkCoordToMapTileChunkCoord(chunkPosZ);
                if (mapTileChunkX < minViewMapTileChunkCoordX || mapTileChunkX > maxViewMapTileChunkCoordX) continue;
                if (mapTileChunkZ < minViewMapTileChunkCoordZ || mapTileChunkZ > maxViewMapTileChunkCoordZ) continue;
                helper.addColoredRectToExistingBuffer(
                    matrix, overlayBufferBuilder,
                    chunkPosX, chunkPosZ, 1, 1,
                    r, g, b, a
                );
            }
        }
    }

    public synchronized void drawMinimapFeaturesShader(
        final PoseStack matrixStack
    ) {
        XaeroPlusShaders.ensureShaders();
        var shader = XaeroPlusShaders.HIGHLIGHT_SHADER;
        if (shader == null) return;
        shader.setWorldMapViewMatrix(matrixStack.last().pose());
        RenderSystem.enableBlend();
        for (int i = 0; i < sortedKeySet.size(); i++) {
            var k = sortedKeySet.get(i);
            if (k == null) continue;
            var feature = chunkHighlightDrawFeatures.get(k);
            if (feature == null) continue;
            int color = feature.colorInt();
            var a = ColorHelper.getA(color);
            if (a == 0.0f) return;
            var r = ColorHelper.getR(color);
            var g = ColorHelper.getG(color);
            var b = ColorHelper.getB(color);
            shader.setHighlightColor(r, g, b, a);
            var highlights = feature.getChunkHighlights();
            var drawBuffer = feature.getHighlightDrawBuffer();
            if (drawBuffer.needsRefresh()) {
                drawBuffer.refresh(highlights);
            }
            drawBuffer.render();
        }
        RenderSystem.disableBlend();
    }

    public synchronized void drawWorldMapFeatures(
        final double minBlockX,
        final double maxBlockX,
        final double minBlockZ,
        final double maxBlockZ,
        final int flooredCameraX,
        final int flooredCameraZ,
        final PoseStack matrixStack,
        final VertexConsumer overlayBuffer
    ) {
        if (XaeroPlusSettingRegistry.highlightShader.getValue())
            drawWorldMapFeaturesShader(flooredCameraX, flooredCameraZ, matrixStack);
        else
            drawWorldMapFeaturesImmediate(minBlockX, maxBlockX, minBlockZ, maxBlockZ, flooredCameraX, flooredCameraZ, matrixStack, overlayBuffer);
    }

    public synchronized void drawWorldMapFeaturesShader(
        final int flooredCameraX,
        final int flooredCameraZ,
        final PoseStack matrixStack
    ) {
        XaeroPlusShaders.ensureShaders();
        var shader = XaeroPlusShaders.HIGHLIGHT_SHADER;
        if (shader == null) return;
        matrixStack.pushPose();
        matrixStack.translate(-flooredCameraX, -flooredCameraZ, 1.0f);
        matrixStack.scale(16f, -16f, 1f);
        matrixStack.translate(0, -1, 0);
        shader.setWorldMapViewMatrix(matrixStack.last().pose());
        matrixStack.popPose();
        RenderSystem.enableBlend();
        for (int i = 0; i < sortedKeySet.size(); i++) {
            var k = sortedKeySet.get(i);
            if (k == null) continue;
            var feature = chunkHighlightDrawFeatures.get(k);
            if (feature == null) continue;
            int color = feature.colorInt();
            var a = ColorHelper.getA(color);
            if (a == 0.0f) return;
            var r = ColorHelper.getR(color);
            var g = ColorHelper.getG(color);
            var b = ColorHelper.getB(color);
            shader.setHighlightColor(r, g, b, a);
            var highlights = feature.getChunkHighlights();
            if (feature.getHighlightDrawBuffer().needsRefresh()) {
                feature.getHighlightDrawBuffer().refresh(highlights);
            }
            feature.getHighlightDrawBuffer().render();
        }
        RenderSystem.disableBlend();
    }

    public synchronized void drawWorldMapFeaturesImmediate(
        final double minBlockX,
        final double maxBlockX,
        final double minBlockZ,
        final double maxBlockZ,
        final int flooredCameraX,
        final int flooredCameraZ,
        final PoseStack matrixStack,
        final VertexConsumer overlayBuffer
    ) {
        matrixStack.pushPose();
        matrixStack.translate(-flooredCameraX, -flooredCameraZ, 0);
        var matrix = matrixStack.last().pose();
        for (int i = 0; i < sortedKeySet.size(); i++) {
            var k = sortedKeySet.get(i);
            if (k == null) continue;
            var feature = chunkHighlightDrawFeatures.get(k);
            if (feature == null) continue;
            feature.getHighlightDrawBuffer().close();
            int color = feature.colorInt();
            var a = ColorHelper.getA(color);
            if (a == 0.0f) return;
            var r = ColorHelper.getR(color);
            var g = ColorHelper.getG(color);
            var b = ColorHelper.getB(color);
            var highlights = feature.getChunkHighlights();
            for (int j = 0; j < highlights.size(); j++) {
                long highlight = highlights.getLong(j);
                var chunkPosX = ChunkUtils.longToChunkX(highlight);
                var chunkPosZ = ChunkUtils.longToChunkZ(highlight);
                var blockX = ChunkUtils.chunkCoordToCoord(chunkPosX);
                var blockZ = ChunkUtils.chunkCoordToCoord(chunkPosZ);
                if (blockX < minBlockX - 32 || blockX > maxBlockX) continue;
                if (blockZ < minBlockZ - 32 || blockZ > maxBlockZ) continue;
                final float left = (float) ChunkUtils.chunkCoordToCoord(chunkPosX);
                final float top = (float) ChunkUtils.chunkCoordToCoord(chunkPosZ);
                final float right = left + 16;
                final float bottom = top + 16;
                MinimapBackgroundDrawHelper.fillIntoExistingBuffer(
                    matrix, overlayBuffer,
                    left, top, right, bottom,
                    r, g, b, a
                );
            }
        }
        matrixStack.popPose();
    }
}
