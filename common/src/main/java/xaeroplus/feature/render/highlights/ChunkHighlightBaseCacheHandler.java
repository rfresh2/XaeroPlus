package xaeroplus.feature.render.highlights;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import static xaeroplus.util.ChunkUtils.chunkPosToLong;

public abstract class ChunkHighlightBaseCacheHandler implements ChunkHighlightCache {
    public final Long2LongMap chunks = new Long2LongOpenHashMap();
    Minecraft mc = Minecraft.getInstance();

    public ChunkHighlightBaseCacheHandler() {
        this.chunks.defaultReturnValue(-1);
    }

    @Override
    public void addHighlight(final int x, final int z) {
        addHighlight(x, z, System.currentTimeMillis());
    }

    public void addHighlight(final int x, final int z, final long foundTime) {
        if (!mc.isSameThread()) {
            throw new RuntimeException("addHighlight must be called on the main thread!");
        }
        chunks.put(chunkPosToLong(x, z), foundTime);
    }

    @Override
    public void removeHighlight(final int x, final int z) {
        if (!mc.isSameThread()) {
            throw new RuntimeException("removeHighlight must be called on the main thread!");
        }
        chunks.remove(chunkPosToLong(x, z));
    }

    @Override
    public boolean isHighlighted(final int x, final int z, ResourceKey<Level> dimensionId) {
        return isHighlighted(chunkPosToLong(x, z));
    }

    @Override
    public Long2LongMap getCacheMap(final ResourceKey<Level> dimension) {
        return chunks;
    }

    public boolean isHighlighted(final long chunkPos) {
        return chunks.containsKey(chunkPos);
    }

    public void replaceState(final Long2LongOpenHashMap state) {
        if (!mc.isSameThread()) {
            throw new RuntimeException("replaceState must be called on the main thread!");
        }
        this.chunks.clear();
        this.chunks.putAll(state);
    }

    public void reset() {
        if (!mc.isSameThread()) {
            throw new RuntimeException("reset must be called on the main thread!");
        }
        chunks.clear();
    }
}
