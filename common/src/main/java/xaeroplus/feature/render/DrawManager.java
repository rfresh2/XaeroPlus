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
import xaero.common.graphics.shader.FramebufferLinesShaderHelper;
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
        var mc = Minecraft.getInstance();
        FramebufferLinesShaderHelper.setFrameSize(mc.getWindow().getWidth(), mc.getWindow().getHeight());
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

    public void drawChunkHighlights(final PoseStack matrixStack, final boolean worldmap) {
        var shader = Minecraft.getInstance()
            .getShaderManager()
            .getProgram(XaeroPlusShaders.HIGHLIGHT_SHADER_PROGRAM);
        XaeroPlusShaders.setMapViewMatrix(matrixStack.last().pose());
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
            XaeroPlusShaders.setHighlightColor(r, g, b, a);
            feature.render(worldmap);
        });
        RenderSystem.disableBlend();
    }
}
