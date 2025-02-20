package xaeroplus.feature.render.highlights;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaeroplus.XaeroPlus;
import xaeroplus.event.XaeroWorldChangeEvent;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ChunkHighlightLocalCache extends ChunkHighlightBaseCacheHandler {
    private static final int maxNumber = 5000;

    public ChunkHighlightLocalCache() {
        super();
    }

    @Override
    public void addHighlight(final int x, final int z) {
        super.addHighlight(x, z);

        limitChunksSize();
    }

    @Override
    public void addHighlight(final int x, final int z, final long foundTime) {
        super.addHighlight(x, z, foundTime);
        limitChunksSize();
    }

    private void limitChunksSize() {
        try {
            if (chunks.size() > maxNumber) {
                var toRemove = chunks.long2LongEntrySet().stream()
                    .sorted(Map.Entry.comparingByValue())
                    .limit(500)
                    .mapToLong(Long2LongMap.Entry::getLongKey)
                    .toArray();
                for (int i = 0; i < toRemove.length; i++) {
                    chunks.remove(toRemove[i]);
                }
            }
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error limiting local cache size", e);
        }
    }

    @Override
    public CompletableFuture<Long2LongMap> getHighlightsInCustomWindow(final int windowRegionX, final int windowRegionZ, final int windowRegionSize, final ResourceKey<Level> dimension) {
        return submitTickTask(() -> new Long2LongOpenHashMap(chunks));
    }

    @Override
    public void handleWorldChange(final XaeroWorldChangeEvent event) {
        // intentionally not clearing cache on any world change state
        // its somewhat useful for players so they don't lose all date if they get disconnected somehow
        // but it does lead to data incoherence if they switch dimensions or servers in the same session
        // so its a tradeoff, generally players will always use the database cache anyway
        chunks.clear();
    }

    @Override
    public void handleTick() {}

    @Override
    public void onEnable() {}

    @Override
    public void onDisable() {}
}
