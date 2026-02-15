package xaeroplus.feature.render.line;

import com.mojang.blaze3d.buffers.GpuBuffer;
import org.jetbrains.annotations.Nullable;
import xaeroplus.feature.render.DrawContext;

public abstract class AbstractLineVertexBuffer<T> {
    protected boolean stale = true;
    @Nullable protected GpuBuffer vertexBuffer = null;
    protected boolean flipped = false;
    protected int indexCount = 0;

    public boolean needsRefresh(final DrawContext ctx) {
        return vertexBuffer == null || vertexBuffer.isClosed() || stale || flipped != ctx.worldmap();
    }

    public void preRender(final DrawContext ctx, final T lines) {
        if (needsRefresh(ctx)) {
            refresh(ctx, lines);
        }
    }

    protected abstract void refresh(DrawContext ctx, T lines);

    public abstract void render(DrawContext ctx, float lineWidthScale);

    public void markStale() {
        stale = true;
    }

    public void close() {
        if (vertexBuffer != null) {
            vertexBuffer.close();
            vertexBuffer = null;
        }
    }
}
