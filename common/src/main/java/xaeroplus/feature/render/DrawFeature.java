package xaeroplus.feature.render;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import it.unimi.dsi.fastutil.longs.LongList;

// internal class to hold the highlight provider and cache
record DrawFeature(
    ChunkHighlightProvider chunkHighlightProvider,
    AsyncLoadingCache<Long, LongList> chunkRenderCache
) {
    public LongList getChunkHighlights(final long regionLong) {
        return chunkRenderCache.get(regionLong).getNow(LongList.of());
    }
}
