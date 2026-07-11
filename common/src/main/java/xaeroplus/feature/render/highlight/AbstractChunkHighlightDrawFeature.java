package xaeroplus.feature.render.highlight;

import xaeroplus.feature.render.DrawContext;
import xaeroplus.feature.render.DrawFeature;

public abstract class AbstractChunkHighlightDrawFeature implements DrawFeature {
    public final AbstractHighlightVertexBuffer drawBuffer;

    protected AbstractChunkHighlightDrawFeature(final AbstractHighlightVertexBuffer drawBuffer) {
        this.drawBuffer = drawBuffer;
    }

    public void preRender(final DrawContext ctx) {

    }

    public void postRender(final DrawContext ctx) {

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
