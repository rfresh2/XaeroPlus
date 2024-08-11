package xaeroplus.feature.render;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import it.unimi.dsi.fastutil.longs.LongSet;

public record WindowedDrawFeature(
    ChunkHighlightProvider chunkHighlightProvider,
    AsyncLoadingCache<Long, LongSet> chunkRenderCache
) {
    public LongSet getChunkHighlights() {
        return chunkRenderCache.get(0L).getNow(LongSet.of());
    }
}
