package xaeroplus.util.newchunks;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import xaeroplus.util.highlights.ChunkHighlightLocalCache;
import xaeroplus.util.highlights.HighlightAtChunkPos;

import java.util.Collections;
import java.util.List;

import static xaeroplus.util.ChunkUtils.getActualDimension;

public class NewChunksLocalCache implements NewChunksCache {
    private final ChunkHighlightLocalCache delegate = new ChunkHighlightLocalCache();

    @Override
    public void addNewChunk(final int x, final int z) {
        delegate.addHighlight(x, z);
    }

    @Override
    public void addNewChunk(final int x, final int z, final long foundTime, final int dimensionId) {
        delegate.addHighlight(x, z, foundTime);
    }

    @Override
    public boolean isNewChunk(final int chunkPosX, final int chunkPosZ, final int dimensionId) {
        // local cache doesn't support cross-dimensional lookups
        if (dimensionId != getActualDimension()) return false;
        return delegate.isHighlighted(chunkPosX, chunkPosZ);
    }

    @Override
    public List<HighlightAtChunkPos> getNewChunksInRegion(final int leafRegionX, final int leafRegionZ, final int level, final int dimension) {
        if (dimension != getActualDimension()) return Collections.emptyList();
        return delegate.getHighlightsInRegion(leafRegionX, leafRegionZ, level);
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
        return delegate.getHighlightsState();
    }

    @Override
    public void loadPreviousState(final Long2LongOpenHashMap state) {
        delegate.loadPreviousState(state);
    }
}
