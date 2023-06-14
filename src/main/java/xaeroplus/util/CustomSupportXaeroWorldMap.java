package xaeroplus.util;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import xaero.common.XaeroMinimapSession;
import xaero.common.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.common.minimap.render.MinimapRendererHelper;

public interface CustomSupportXaeroWorldMap {
    void drawMinimapWithDrawContext(
            DrawContext guiGraphics,
            XaeroMinimapSession minimapSession,
            MatrixStack matrixStack,
            MinimapRendererHelper helper,
            int xFloored,
            int zFloored,
            int minViewX,
            int minViewZ,
            int maxViewX,
            int maxViewZ,
            boolean zooming,
            double zoom,
            VertexConsumer overlayBufferBuilder,
            MultiTextureRenderTypeRendererProvider multiTextureRenderTypeRenderers);
}
