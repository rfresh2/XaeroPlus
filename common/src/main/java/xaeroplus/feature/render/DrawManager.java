package xaeroplus.feature.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
        matrixStack.popPose();
        drawMinimapLines(matrixStack, renderTypeBuffers);
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
                int x1 = line.x1();
                int z1 = line.z1();
                int x2 = line.x2();
                int z2 = line.z2();
                if (z2 < z1) {
                    int tz1 = z1;
                    z1 = z2;
                    z2 = tz1;
                    int tx1 = x1;
                    x1 = x2;
                    x2 = tx1;
                }
                DrawHelper.addColoredLineToExistingBuffer(
                    matrixStack.last(), lineBuffer,
                    x1, z1,
                    x2, z2,
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
        matrixStack.popPose();
        drawWorldMapLines(matrixStack, fboScale, renderTypeBuffers);
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
                int x1 = line.x1();
                int z1 = line.z1();
                int x2 = line.x2();
                int z2 = line.z2();
                if (z2 < z1) {
                    int tz1 = z1;
                    z1 = z2;
                    z2 = tz1;
                    int tx1 = x1;
                    x1 = x2;
                    x2 = tx1;
                }
                DrawHelper.addColoredLineToExistingBuffer(
                    matrixStack.last(), lineBuffer,
                    x2, z2,
                    x1, z1,
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
        RenderSystem.blendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
        );
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
}
