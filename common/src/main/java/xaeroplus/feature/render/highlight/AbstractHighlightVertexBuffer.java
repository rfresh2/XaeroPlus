package xaeroplus.feature.render.highlight;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import xaeroplus.feature.render.CachedVertexBuffer;
import xaeroplus.feature.render.DrawContext;

public abstract class AbstractHighlightVertexBuffer extends CachedVertexBuffer {
    public long lastRefreshed = 0L;

    public void preRender(final DrawContext ctx, final Long2LongMap highlights, final int color) {
        if (needsRefresh(ctx)) {
            refresh(ctx, highlights, color);
        }
    }

    public abstract void refresh(DrawContext ctx, Long2LongMap highlights, int color);
}
