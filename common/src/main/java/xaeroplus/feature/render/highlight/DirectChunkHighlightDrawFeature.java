package xaeroplus.feature.render.highlight;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import xaeroplus.Globals;
import xaeroplus.feature.render.DrawContext;

import java.util.concurrent.ThreadLocalRandom;

public class DirectChunkHighlightDrawFeature extends AbstractChunkHighlightDrawFeature {
    private final String id;
    private final DirectChunkHighlightProvider chunkHighlightProvider;
    private int lastRefreshedHighlightCount = 0;
    private int jitterOffset = ThreadLocalRandom.current().nextInt(0, 100);
    private final int refreshIntervalMs;

    public DirectChunkHighlightDrawFeature(String id, AbstractHighlightVertexBuffer drawBuffer, DirectChunkHighlightProvider chunkHighlightProvider, int refreshIntervalMs) {
        super(drawBuffer);
        this.id = id;
        this.chunkHighlightProvider = chunkHighlightProvider;
        this.refreshIntervalMs = refreshIntervalMs;
    }

    public int color() {
        return chunkHighlightProvider.colorSupplier().getAsInt();
    }

    public Long2LongMap chunkHighlights() {
        return chunkHighlightProvider.chunkHighlightSupplier().getHighlights(Globals.getCurrentDimensionId());
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public void preRender(final DrawContext ctx) {
        super.preRender(ctx);
        Long2LongMap highlights = chunkHighlights();
        if (System.currentTimeMillis() - drawBuffer.lastRefreshed >= refreshIntervalMs) {
            // doing this means our configured refresh interval ms is more of a lower bound than the actual interval
            // but reducing refreshes significantly improves performance at large highlight counts
            // and even worse is all features refreshing on the same tick, potentially causing a noticeable lag spike
            boolean highlightCountChanged = lastRefreshedHighlightCount != highlights.size();
            boolean refreshRegardlessThreshold = System.currentTimeMillis() - drawBuffer.lastRefreshed > 500L + jitterOffset;
            if (highlightCountChanged || refreshRegardlessThreshold) {
                this.invalidateCache();
                lastRefreshedHighlightCount = highlights.size();
                jitterOffset = ThreadLocalRandom.current().nextInt(0, 100);
            }
        }
        drawBuffer.preRender(ctx, highlights, color());
    }

    @Override
    public void render(final DrawContext ctx) {
        preRender(ctx);
        drawBuffer.render();
        postRender(ctx);
    }
}
