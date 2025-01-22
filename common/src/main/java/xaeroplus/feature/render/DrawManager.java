package xaeroplus.feature.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.lenni0451.lambdaevents.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import xaero.common.HudMod;
import xaero.common.graphics.CustomRenderTypes;
import xaero.common.graphics.shader.MinimapShaders;
import xaero.common.minimap.render.MinimapRendererHelper;
import xaeroplus.Globals;
import xaeroplus.XaeroPlus;
import xaeroplus.event.XaeroWorldChangeEvent;
import xaeroplus.settings.Settings;
import xaeroplus.util.ChunkUtils;
import xaeroplus.util.ColorHelper;
import xaeroplus.util.FloatSupplier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.IntSupplier;

public class DrawManager {
    private final Object2ObjectMap<String, ChunkHighlightDrawFeature> chunkHighlightDrawFeatures = new Object2ObjectOpenHashMap<>();
    private final Object2ObjectMap<String, LineDrawFeature> lineDrawFeatures = new Object2ObjectOpenHashMap<>();
    private final List<String> sortedChunkHighlightKeySet = new ArrayList<>();
    private final List<String> sortedLineKeySet = new ArrayList<>();

    public DrawManager() {
        XaeroPlus.EVENT_BUS.register(this);
    }

    public synchronized void registerChunkHighlightProvider(String id, ChunkHighlightSupplier chunkHighlightSupplier, IntSupplier colorSupplier) {
        unregisterChunkHighlightProvider(id); // just in case
        chunkHighlightDrawFeatures.put(id, new ChunkHighlightDrawFeature(new ChunkHighlightProvider(chunkHighlightSupplier, colorSupplier)));
        sortedChunkHighlightKeySet.add(id);
        // arbitrary order, just needs to be consistent so colors blend consistently
        sortedChunkHighlightKeySet.sort(Comparator.naturalOrder());
    }

    public synchronized void unregisterChunkHighlightProvider(String id) {
        sortedChunkHighlightKeySet.remove(id);
        ChunkHighlightDrawFeature feature = chunkHighlightDrawFeatures.remove(id);
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

    public synchronized void registerLineProvider(String id, LineSupplier lineSupplier, IntSupplier colorSupplier, FloatSupplier lineWidthSupplier, int refreshIntervalMs) {
        unregisterLineProvider(id); // just in case
        lineDrawFeatures.put(id, new LineDrawFeature(new LineProvider(lineSupplier, colorSupplier, lineWidthSupplier), refreshIntervalMs));
        sortedLineKeySet.add(id);
        sortedLineKeySet.sort(Comparator.naturalOrder());
    }

    public synchronized void unregisterLineProvider(String id) {
        sortedLineKeySet.remove(id);
        lineDrawFeatures.remove(id);
    }

    @EventHandler
    public void onXaeroWorldChange(XaeroWorldChangeEvent event) {
        chunkHighlightDrawFeatures.values().forEach(ChunkHighlightDrawFeature::invalidateCache);
        lineDrawFeatures.values().forEach(LineDrawFeature::invalidateCache);
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
        MinimapRendererHelper helper,
        final MultiBufferSource.BufferSource renderTypeBuffers) {
        if (HudMod.INSTANCE.isFairPlay()) return;
        matrixStack.pushPose();
        matrixStack.translate(
            -(chunkX * 64) - (tileX * 16) - insideX,
            -(chunkZ * 64) - (tileZ * 16) - insideZ,
            0);
        matrixStack.pushPose();
        matrixStack.scale(16f, 16f, 1f);
        if (Settings.REGISTRY.highlightShader.get()) {
            drawMinimapHighlightsShader(matrixStack);
        } else {
            drawMinimapHighlightsImmediate(minViewMapTileChunkCoordX, maxViewMapTileChunkCoordX, minViewMapTileChunkCoordZ, maxViewMapTileChunkCoordZ,
                                           matrixStack, overlayBufferBuilder, helper);
        }
        matrixStack.popPose();
        drawMinimapLines(matrixStack, renderTypeBuffers);
        matrixStack.popPose();
    }

    public synchronized void drawMinimapLines(
        final PoseStack matrixStack,
        final MultiBufferSource.BufferSource renderTypeBuffers
    ) {
        for (int i = 0; i < sortedLineKeySet.size(); i++) {
            var k = sortedLineKeySet.get(i);
            if (k == null) continue;
            var feature = lineDrawFeatures.get(k);
            if (feature == null) continue;
            int color = feature.colorInt();
            var a = ColorHelper.getA(color);
            if (a == 0.0f) return;
            VertexConsumer lineBuffer = renderTypeBuffers.getBuffer(CustomRenderTypes.MAP_LINES);
            float lineWidthScale = 16f * Mth.clamp(feature.lineWidth(), 0.1f * Globals.minimapScaleMultiplier, 1000.0f);
            RenderSystem.lineWidth(lineWidthScale);
            var r = ColorHelper.getR(color);
            var g = ColorHelper.getG(color);
            var b = ColorHelper.getB(color);
            var lines = feature.getLines();
            for (int j = 0; j < lines.size(); j++) {
                var line = lines.get(j);
                DrawHelper.addColoredLineToExistingBuffer(
                    matrixStack.last(), lineBuffer,
                    line.x1(), line.z1(),
                    line.x2(), line.z2(),
                    r, g, b, a);
            }
            renderTypeBuffers.endBatch(CustomRenderTypes.MAP_LINES);
        }
    }

    public synchronized void drawMinimapHighlightsImmediate(
        int minViewMapTileChunkCoordX,
        int maxViewMapTileChunkCoordX,
        int minViewMapTileChunkCoordZ,
        int maxViewMapTileChunkCoordZ,
        final PoseStack matrixStack,
        final VertexConsumer overlayBufferBuilder,
        MinimapRendererHelper helper
    ) {
        var matrix = matrixStack.last().pose();
        for (int i = 0; i < sortedChunkHighlightKeySet.size(); i++) {
            var k = sortedChunkHighlightKeySet.get(i);
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

    public synchronized void drawMinimapHighlightsShader(
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
        for (int i = 0; i < sortedChunkHighlightKeySet.size(); i++) {
            var k = sortedChunkHighlightKeySet.get(i);
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
        final VertexConsumer overlayBuffer,
        final double fboScale,
        final MultiBufferSource.BufferSource renderTypeBuffers
    ) {
        if (HudMod.INSTANCE.isFairPlay()) return;
        matrixStack.pushPose();
        matrixStack.translate(-flooredCameraX, -flooredCameraZ, 1.0f);
        matrixStack.pushPose();
        matrixStack.scale(16f, 16f, 1f);
        if (Settings.REGISTRY.highlightShader.get())
            drawWorldMapHighlightsShader(matrixStack);
        else
            drawWorldMapHighlightsImmediate(minBlockX, maxBlockX, minBlockZ, maxBlockZ, matrixStack, overlayBuffer);
        matrixStack.popPose();
        drawWorldMapLines(matrixStack, fboScale, renderTypeBuffers);
        matrixStack.popPose();
    }

    public synchronized void drawWorldMapLines(
        final PoseStack matrixStack,
        double fboScale,
        final MultiBufferSource.BufferSource renderTypeBuffers
    ) {
        var mc = Minecraft.getInstance();
        MinimapShaders.FRAMEBUFFER_LINES.setFrameSize(mc.getWindow().getWidth(), mc.getWindow().getHeight());
        for (int i = 0; i < sortedLineKeySet.size(); i++) {
            var k = sortedLineKeySet.get(i);
            if (k == null) continue;
            var feature = lineDrawFeatures.get(k);
            if (feature == null) continue;
            int color = feature.colorInt();
            var a = ColorHelper.getA(color);
            if (a == 0.0f) return;
            VertexConsumer lineBuffer = renderTypeBuffers.getBuffer(CustomRenderTypes.MAP_LINES);
            float lineWidthScale = 16f * (float) Mth.clamp(feature.lineWidth() * fboScale, 0.1f, 1000.0f);
            RenderSystem.lineWidth(lineWidthScale);
            var r = ColorHelper.getR(color);
            var g = ColorHelper.getG(color);
            var b = ColorHelper.getB(color);
            var lines = feature.getLines();
            for (int j = 0; j < lines.size(); j++) {
                var line = lines.get(j);
                DrawHelper.addColoredLineToExistingBuffer(
                    matrixStack.last(), lineBuffer,
                    line.x2(), line.z2(),
                    line.x1(), line.z1(),
                    r, g, b, a);
            }
            renderTypeBuffers.endBatch(CustomRenderTypes.MAP_LINES);
        }
    }

    public synchronized void drawWorldMapHighlightsShader(final PoseStack matrixStack) {
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
        for (int i = 0; i < sortedChunkHighlightKeySet.size(); i++) {
            var k = sortedChunkHighlightKeySet.get(i);
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

    public synchronized void drawWorldMapHighlightsImmediate(
        final double minBlockX,
        final double maxBlockX,
        final double minBlockZ,
        final double maxBlockZ,
        final PoseStack matrixStack,
        final VertexConsumer overlayBuffer
    ) {
        var matrix = matrixStack.last().pose();
        for (int i = 0; i < sortedChunkHighlightKeySet.size(); i++) {
            var k = sortedChunkHighlightKeySet.get(i);
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
                DrawHelper.fillIntoExistingBuffer(
                    matrix, overlayBuffer,
                    left, top, right, bottom,
                    r, g, b, a
                );
            }
        }
    }
}
