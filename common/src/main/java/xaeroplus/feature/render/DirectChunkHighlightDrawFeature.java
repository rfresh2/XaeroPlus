package xaeroplus.feature.render;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import xaeroplus.Globals;

public class DirectChunkHighlightDrawFeature implements ChunkHighlightDrawFeature {
    private final DirectChunkHighlightProvider chunkHighlightProvider;
    private final HighlightDrawBuffer drawBuffer = new HighlightDrawBuffer();
    private static final long REFRESH_INTERVAL_MS = 500L;
    private int lastRefreshedHighlightCount = 0;

    public DirectChunkHighlightDrawFeature(DirectChunkHighlightProvider chunkHighlightProvider) {
        this.chunkHighlightProvider = chunkHighlightProvider;
    }

    @Override
    public int colorInt() {
        return chunkHighlightProvider.colorSupplier().getAsInt();
    }

    @Override
    public void invalidateCache() {
        drawBuffer.markStale();
    }

    @Override
    public Long2LongMap getChunkHighlights() {
        return chunkHighlightProvider.chunkHighlightSupplier().getHighlights(Globals.getCurrentDimensionId());
    }

    @Override
    public void render(boolean worldmap) {
        Long2LongMap highlights = getChunkHighlights();
        if (lastRefreshedHighlightCount != highlights.size()
            && System.currentTimeMillis() - drawBuffer.lastRefreshed > REFRESH_INTERVAL_MS) {
            this.invalidateCache();
            lastRefreshedHighlightCount = highlights.size();
        }
        if (drawBuffer.needsRefresh(worldmap)) {
            drawBuffer.refresh(highlights, worldmap);
        }
        drawBuffer.render();
    }

    @Override
    public void close() {
        drawBuffer.close();
    }
}
