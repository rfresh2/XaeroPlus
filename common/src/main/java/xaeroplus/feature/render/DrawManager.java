package xaeroplus.feature.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import net.lenni0451.lambdaevents.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import xaero.common.HudMod;
import xaero.common.graphics.CustomRenderTypes;
import xaero.common.graphics.shader.MinimapShaders;
import xaeroplus.Globals;
import xaeroplus.XaeroPlus;
import xaeroplus.event.XaeroWorldChangeEvent;
import xaeroplus.util.ColorHelper;

public class DrawManager {
    private final DrawFeatureRegistry registry = new DrawFeatureRegistry();

    public final DrawFeatureRegistry registry() {
        return registry;
    }

    public DrawManager() {
        XaeroPlus.EVENT_BUS.register(this);
    }

    @EventHandler
    public void onXaeroWorldChange(XaeroWorldChangeEvent event) {
        registry.invalidateCaches();
    }

    public void drawMinimapFeatures(
        int chunkX,
        int chunkZ,
        int tileX,
        int tileZ,
        int insideX,
        int insideZ,
        final PoseStack matrixStack,
        final MultiBufferSource.BufferSource renderTypeBuffers
    ) {
        if (HudMod.INSTANCE.isFairPlay()) return;
        matrixStack.pushPose();
        matrixStack.translate(
            -(chunkX * 64) - (tileX * 16) - insideX,
            -(chunkZ * 64) - (tileZ * 16) - insideZ,
            0);
        matrixStack.pushPose();
        matrixStack.scale(16f, 16f, 1f);
        drawChunkHighlights(matrixStack, false);
        drawChunkColoredHighlights(matrixStack, false);
        matrixStack.popPose();
        drawMinimapLines(matrixStack, renderTypeBuffers);
        drawMinimapColoredLines(matrixStack, renderTypeBuffers);
        matrixStack.popPose();
    }

    public void drawMinimapLines(
        final PoseStack matrixStack,
        final MultiBufferSource.BufferSource renderTypeBuffers
    ) {
        MinimapShaders.ensureShaders();
        registry.forEachLineDrawFeature(feature -> {
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
        });
    }

    public void drawMinimapColoredLines(
        final PoseStack matrixStack,
        final MultiBufferSource.BufferSource renderTypeBuffers
    ) {
        MinimapShaders.ensureShaders();
        registry.forEachColoredLineDrawFeature(feature -> {
            var a = feature.colorAlphaInt() / 255.0f;
            if (a == 0.0f) return;
            VertexConsumer lineBuffer = renderTypeBuffers.getBuffer(CustomRenderTypes.MAP_LINES);
            float lineWidthScale = 16f * Mth.clamp(feature.lineWidth(), 0.1f * Globals.minimapScaleMultiplier, 1000.0f);
            RenderSystem.lineWidth(lineWidthScale);
            var lines = feature.getLines();
            var it = Object2IntMaps.fastIterator(lines);
            while (it.hasNext()) {
                var entry = it.next();
                var line = entry.getKey();
                var color = entry.getIntValue();
                var r = ColorHelper.getR(color);
                var g = ColorHelper.getG(color);
                var b = ColorHelper.getB(color);
                DrawHelper.addColoredLineToExistingBuffer(
                    matrixStack.last(), lineBuffer,
                    line.x1(), line.z1(),
                    line.x2(), line.z2(),
                    r, g, b, a);
            }
            renderTypeBuffers.endBatch(CustomRenderTypes.MAP_LINES);
        });
    }

    public void drawWorldMapFeatures(
        final int flooredCameraX,
        final int flooredCameraZ,
        final PoseStack matrixStack,
        final double fboScale,
        final MultiBufferSource.BufferSource renderTypeBuffers
    ) {
        if (HudMod.INSTANCE.isFairPlay()) return;
        matrixStack.pushPose();
        matrixStack.translate(-flooredCameraX, -flooredCameraZ, 1.0f);
        matrixStack.pushPose();
        matrixStack.scale(16f, 16f, 1f);
        drawChunkHighlights(matrixStack, true);
        drawChunkColoredHighlights(matrixStack, true);
        matrixStack.popPose();
        drawWorldMapLines(matrixStack, fboScale, renderTypeBuffers);
        drawWorldMapColoredLines(matrixStack, fboScale, renderTypeBuffers);
        matrixStack.popPose();
    }

    public void drawWorldMapLines(
        final PoseStack matrixStack,
        final double fboScale,
        final MultiBufferSource.BufferSource renderTypeBuffers
    ) {
        MinimapShaders.ensureShaders();
        var mc = Minecraft.getInstance();
        MinimapShaders.FRAMEBUFFER_LINES.setFrameSize(mc.getWindow().getWidth(), mc.getWindow().getHeight());
        registry.forEachLineDrawFeature(feature -> {
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
        });
    }

    public void drawWorldMapColoredLines(
        final PoseStack matrixStack,
        final double fboScale,
        final MultiBufferSource.BufferSource renderTypeBuffers
    ) {
        MinimapShaders.ensureShaders();
        var mc = Minecraft.getInstance();
        MinimapShaders.FRAMEBUFFER_LINES.setFrameSize(mc.getWindow().getWidth(), mc.getWindow().getHeight());
        registry.forEachColoredLineDrawFeature(feature -> {
            var a = feature.colorAlphaInt() / 255.0f;
            if (a == 0) return;
            VertexConsumer lineBuffer = renderTypeBuffers.getBuffer(CustomRenderTypes.MAP_LINES);
            float lineWidthScale = 16f * (float) Mth.clamp(feature.lineWidth() * fboScale, 0.1f, 1000.0f);
            RenderSystem.lineWidth(lineWidthScale);
            var lines = feature.getLines();
            var it = Object2IntMaps.fastIterator(lines);
            while (it.hasNext()) {
                var entry = it.next();
                var line = entry.getKey();
                var color = entry.getIntValue();
                var r = ColorHelper.getR(color);
                var g = ColorHelper.getG(color);
                var b = ColorHelper.getB(color);
                DrawHelper.addColoredLineToExistingBuffer(
                    matrixStack.last(), lineBuffer,
                    line.x2(), line.z2(),
                    line.x1(), line.z1(),
                    r, g, b, a);
            }
            renderTypeBuffers.endBatch(CustomRenderTypes.MAP_LINES);
        });
    }

    public void drawChunkHighlights(final PoseStack matrixStack, final boolean worldmap) {
        XaeroPlusShaders.ensureShaders();
        var shader = XaeroPlusShaders.HIGHLIGHT_SHADER;
        if (shader == null) return;
        shader.setMapViewMatrix(matrixStack.last().pose());
        RenderSystem.enableBlend();
        registry.forEachChunkHighlightDrawFeature(feature -> {
            int color = feature.colorInt();
            var a = ColorHelper.getA(color);
            if (a == 0.0f) return;
            var r = ColorHelper.getR(color);
            var g = ColorHelper.getG(color);
            var b = ColorHelper.getB(color);
            shader.setHighlightColor(r, g, b, a);
            feature.render(worldmap);
        });
        RenderSystem.disableBlend();
    }

    public void drawChunkColoredHighlights(final PoseStack matrixStack, final boolean worldmap) {
        XaeroPlusShaders.ensureShaders();
        var shader = XaeroPlusShaders.COLOR_HIGHLIGHT_SHADER;
        if (shader == null) return;
        shader.setMapViewMatrix(matrixStack.last().pose());
        RenderSystem.enableBlend();
        registry.forEachChunkColoredHighlightDrawFeature(feature -> {
            feature.render(worldmap);
        });
        RenderSystem.disableBlend();
    }
}
