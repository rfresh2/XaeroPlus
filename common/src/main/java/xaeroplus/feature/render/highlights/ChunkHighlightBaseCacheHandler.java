package xaeroplus.feature.render.highlights;

import it.unimi.dsi.fastutil.longs.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import static xaeroplus.util.ChunkUtils.chunkPosToLong;

public abstract class ChunkHighlightBaseCacheHandler implements ChunkHighlightCache {
    public final Long2LongMap chunks = Long2LongMaps.synchronize(new Long2LongOpenHashMap());

    @Override
    public boolean addHighlight(final int x, final int z) {
        return addHighlight(x, z, System.currentTimeMillis());
    }

    public boolean addHighlight(final int x, final int z, final long foundTime) {
        final long chunkPos = chunkPosToLong(x, z);
        chunks.put(chunkPos, foundTime);
        return true;
    }

    @Override
    public boolean removeHighlight(final int x, final int z) {
        final long chunkPos = chunkPosToLong(x, z);
        chunks.remove(chunkPos);
        return true;
    }

    @Override
    public boolean isHighlighted(final int x, final int z, ResourceKey<Level> dimensionId) {
        return isHighlighted(chunkPosToLong(x, z));
    }

    @Override
    public LongList getHighlightsSnapshot(final ResourceKey<Level> dimension) {
        return new LongArrayList(chunks.keySet());
    }

    public boolean isHighlighted(final long chunkPos) {
        return chunks.containsKey(chunkPos);
    }

    @Override
    public Long2LongMap getHighlightsState() {
        return chunks;
    }

    @Override
    public void loadPreviousState(final Long2LongMap state) {
        if (state == null) return;
        chunks.putAll(state);
    }

    public void replaceState(final Long2LongOpenHashMap state) {
        chunks.clear();
        chunks.putAll(state);
    }

    public void reset() {
        chunks.clear();
    }
}
