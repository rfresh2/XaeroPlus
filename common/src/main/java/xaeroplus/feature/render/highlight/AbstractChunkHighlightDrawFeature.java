package xaeroplus.feature.render.highlight;

import xaeroplus.feature.render.DrawContext;
import xaeroplus.feature.render.DrawFeature;

public abstract class AbstractChunkHighlightDrawFeature implements DrawFeature {
    public final AbstractHighlightVertexBuffer drawBuffer;

    protected AbstractChunkHighlightDrawFeature(final AbstractHighlightVertexBuffer drawBuffer) {
        this.drawBuffer = drawBuffer;
    }

    @Override
    public void preRender(final DrawContext ctx) {
        var matrixStack = ctx.matrixStack();
        matrixStack.pushPose();
        matrixStack.scale(16f, 16f, 1f);
    }

    @Override
    public void postRender(final DrawContext ctx) {
        var matrixStack = ctx.matrixStack();
        matrixStack.popPose();
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
