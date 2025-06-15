package xaeroplus.feature.render.highlight;

import com.mojang.blaze3d.systems.RenderSystem;
import xaeroplus.feature.render.DrawContext;
import xaeroplus.feature.render.DrawFeature;
import xaeroplus.feature.render.shaders.XaeroPlusShaders;

public abstract class AbstractChunkHighlightDrawFeature implements DrawFeature {
    public final AbstractHighlightVertexBuffer drawBuffer;

    protected AbstractChunkHighlightDrawFeature(final AbstractHighlightVertexBuffer drawBuffer) {
        this.drawBuffer = drawBuffer;
    }

    public void preRender(final DrawContext ctx) {
        var matrixStack = ctx.matrixStack();
        XaeroPlusShaders.ensureShaders();
        RenderSystem.enableBlend();
        matrixStack.pushPose();
        matrixStack.scale(16f, 16f, 1f);
    }

    public void postRender(final DrawContext ctx) {
        var matrixStack = ctx.matrixStack();
        matrixStack.popPose();
        RenderSystem.disableBlend();
    }

    @Override
    public void invalidateCache() {
        drawBuffer.markStale();
    }

    @Override
    public void close() {
        drawBuffer.close();
    }

}
