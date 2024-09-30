package xaeroplus.feature.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.lenni0451.lambdaevents.EventHandler;
import net.minecraft.client.Minecraft;
import xaero.common.HudMod;
import xaero.common.minimap.render.MinimapRendererHelper;
import xaeroplus.XaeroPlus;
import xaeroplus.event.DimensionSwitchEvent;
import xaeroplus.event.XaeroWorldChangeEvent;
import xaeroplus.settings.Settings;
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
            Minecraft.getInstance().execute(feature::closeDrawBuffers);
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
        if (HudMod.INSTANCE.isFairPlay()) return;
        matrixStack.pushPose();
        matrixStack.translate(
            -(chunkX * 64) - (tileX * 16) - insideX,
            -(chunkZ * 64) - (tileZ * 16) - insideZ,
            0);
        matrixStack.scale(16f, 16f, 1f);
        if (Settings.REGISTRY.highlightShader.get()) {
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
            feature.closeDrawBuffers();
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
        shader.setMapViewMatrix(matrixStack.last().pose());
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.blendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
        );
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
            var drawBuffer = feature.getDrawBuffer();
            if (drawBuffer.needsRefresh(false)) {
                drawBuffer.refresh(highlights, false);
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
        if (HudMod.INSTANCE.isFairPlay()) return;
        matrixStack.pushPose();
        matrixStack.translate(-flooredCameraX, -flooredCameraZ, 1.0f);
        matrixStack.scale(16f, 16f, 1f);
        if (Settings.REGISTRY.highlightShader.get())
            drawWorldMapFeaturesShader(matrixStack);
        else
            drawWorldMapFeaturesImmediate(minBlockX, maxBlockX, minBlockZ, maxBlockZ,
                                          matrixStack, overlayBuffer);
        matrixStack.popPose();
    }

    public synchronized void drawWorldMapFeaturesShader(
        final PoseStack matrixStack
    ) {
        XaeroPlusShaders.ensureShaders();
        var shader = XaeroPlusShaders.HIGHLIGHT_SHADER;
        if (shader == null) return;
        shader.setMapViewMatrix(matrixStack.last().pose());
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
        );
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
            var drawBuffer = feature.getDrawBuffer();
            if (drawBuffer.needsRefresh(true)) {
                drawBuffer.refresh(highlights, true);
            }
            drawBuffer.render();
        }
        RenderSystem.disableBlend();
    }

    public synchronized void drawWorldMapFeaturesImmediate(
        final double minBlockX,
        final double maxBlockX,
        final double minBlockZ,
        final double maxBlockZ,
        final PoseStack matrixStack,
        final VertexConsumer overlayBuffer
    ) {
        var matrix = matrixStack.last().pose();
        for (int i = 0; i < sortedKeySet.size(); i++) {
            var k = sortedKeySet.get(i);
            if (k == null) continue;
            var feature = chunkHighlightDrawFeatures.get(k);
            if (feature == null) continue;
            feature.closeDrawBuffers();
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
                final float left = chunkPosX;
                final float top = chunkPosZ;
                final float right = left + 1;
                final float bottom = top + 1;
                MinimapBackgroundDrawHelper.fillIntoExistingBuffer(
                    matrix, overlayBuffer,
                    left, top, right, bottom,
                    r, g, b, a
                );
            }
        }
    }
}
