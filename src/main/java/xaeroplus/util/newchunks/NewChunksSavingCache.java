package xaeroplus.util.newchunks;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
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
    public Long2LongOpenHashMap getNewChunksState() {
        return delegate.getCacheForCurrentDimension().map(ChunkHighlightCacheDimensionHandler::getHighlightsState).orElse(new Long2LongOpenHashMap());
    }

    @Override
    public void loadPreviousState(final Long2LongOpenHashMap state) {
        delegate.getCacheForCurrentDimension().ifPresent(c -> c.loadPreviousState(state));
    }
}
