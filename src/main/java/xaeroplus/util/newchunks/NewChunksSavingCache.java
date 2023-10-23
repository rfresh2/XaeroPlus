package xaeroplus.util.newchunks;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongMaps;
import xaeroplus.util.highlights.ChunkHighlightCacheDimensionHandler;
import xaeroplus.util.highlights.ChunkHighlightSavingCache;
import xaeroplus.util.highlights.HighlightAtChunkPos;

import java.util.List;

public class NewChunksSavingCache implements NewChunksCache {
    private final ChunkHighlightSavingCache delegate;

    public NewChunksSavingCache(final String databaseName) {
        this.delegate = new ChunkHighlightSavingCache(databaseName);
    }

    @Override
    public void addNewChunk(final int x, final int z) {
        delegate.addHighlight(x, z);
    }

    @Override
    public void addNewChunk(final int x, final int z, final long foundTime, final int dimensionId) {
        delegate.addHighlight(x, z, foundTime, dimensionId);
    }

    @Override
    public boolean isNewChunk(final int chunkPosX, final int chunkPosZ, final int dimensionId) {
        return delegate.isHighlighted(chunkPosX, chunkPosZ, dimensionId);
    }

    @Override
    public List<HighlightAtChunkPos> getNewChunksInRegion(final int leafRegionX, final int leafRegionZ, final int level, final int dimension) {
        return delegate.getHighlightsInRegion(leafRegionX, leafRegionZ, level, dimension);
    }

    @Override
    public void handleWorldChange() {
        delegate.handleWorldChange();
    }

    @Override
    public void handleTick() {
        delegate.handleTick();
    }

    @Override
    public void onEnable() {
        delegate.onEnable();
    }

    @Override
    public void onDisable() {
        delegate.onDisable();
    }

    @Override
    public Long2LongMap getNewChunksState() {
        ChunkHighlightCacheDimensionHandler cacheForCurrentDimension = delegate.getCacheForCurrentDimension();
        if (cacheForCurrentDimension != null) return cacheForCurrentDimension.getHighlightsState();
        return Long2LongMaps.EMPTY_MAP;
    }

    @Override
    public void loadPreviousState(final Long2LongMap state) {
        if (state == null) return;
        ChunkHighlightCacheDimensionHandler cacheForCurrentDimension = delegate.getCacheForCurrentDimension();
        if (cacheForCurrentDimension != null) cacheForCurrentDimension.loadPreviousState(state);
    }
}
