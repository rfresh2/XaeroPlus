package xaeroplus.feature.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.vertex.VertexFormat;
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
    public void refreshIfNeeded(final boolean worldmap) {
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
    }

    @Override
    public void render(boolean worldmap, final RenderPass pass, final GpuBuffer indexBuffer, final VertexFormat.IndexType indexType) {
        drawBuffer.render(pass, indexBuffer, indexType);
    }

    @Override
    public int indexCount() {
        return drawBuffer.indexCount();
    }

    @Override
    public void close() {
        drawBuffer.close();
    }
}
