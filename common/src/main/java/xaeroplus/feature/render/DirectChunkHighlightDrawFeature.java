package xaeroplus.feature.render;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import xaeroplus.Globals;

import java.util.concurrent.ThreadLocalRandom;

public class DirectChunkHighlightDrawFeature implements ChunkHighlightDrawFeature {
    private final DirectChunkHighlightProvider chunkHighlightProvider;
    private final HighlightDrawBuffer drawBuffer = new HighlightDrawBuffer();
    private int lastRefreshedHighlightCount = 0;
    private final boolean refreshEveryTick;

    public DirectChunkHighlightDrawFeature(DirectChunkHighlightProvider chunkHighlightProvider, boolean refreshEveryTick) {
        this.chunkHighlightProvider = chunkHighlightProvider;
        this.refreshEveryTick = refreshEveryTick;
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
        if (refreshEveryTick) {
            if (System.currentTimeMillis() - drawBuffer.lastRefreshed >= 50L) {
                this.invalidateCache();
            }
        } else {
            if (lastRefreshedHighlightCount != highlights.size()
                && System.currentTimeMillis() - drawBuffer.lastRefreshed > 500L + ThreadLocalRandom.current().nextInt(0, 100)) {
                this.invalidateCache();
                lastRefreshedHighlightCount = highlights.size();
            }
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
