package xaeroplus.feature.render.highlights;

import xaeroplus.XaeroPlus;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ChunkHighlightLocalCache extends ChunkHighlightBaseCacheHandler {
    private static final int maxNumber = 5000;

    @Override
    public boolean addHighlight(final int x, final int z) {
        limitChunksSize();
        super.addHighlight(x, z);
        return true;
    }

    @Override
    public boolean addHighlight(final int x, final int z, final long foundTime) {
        limitChunksSize();
        super.addHighlight(x, z, foundTime);
        return true;
    }

    private void limitChunksSize() {
        try {
            if (chunks.size() > maxNumber) {
                if (lock.readLock().tryLock(1, TimeUnit.SECONDS)) {
                    // remove oldest 500 chunks
                    final List<Long> toRemove = chunks.long2LongEntrySet().stream()
                            .sorted(Map.Entry.comparingByValue())
                            .limit(500)
                            .map(Map.Entry::getKey)
                            .collect(Collectors.toList());
                    lock.readLock().unlock();
                    if (lock.writeLock().tryLock(1, TimeUnit.SECONDS)) {
                        toRemove.forEach(l -> chunks.remove((long) l));
                        lock.writeLock().unlock();
                    }
                }
            }
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error limiting local cache size", e);
        }
    }

    @Override
    public void handleWorldChange() {}

    @Override
    public void handleTick() {}

    @Override
    public void onEnable() {}

    @Override
    public void onDisable() {}
}
