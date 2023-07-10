package xaeroplus.util.newchunks;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import xaeroplus.XaeroPlus;
import xaeroplus.util.HighlightAtChunkPos;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import static xaeroplus.util.ChunkUtils.getActualDimension;

public class NewChunksLocalCache extends NewChunksBaseCacheHandler implements NewChunksCache {
    private final Long2LongOpenHashMap chunks = new Long2LongOpenHashMap();
    private static final int maxNumber = 5000;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    @Override
    public void addNewChunk(final int x, final int z) {
        limitChunksSize();
        super.addNewChunk(x, z);
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
    public boolean isNewChunk(final int chunkPosX, final int chunkPosZ, final int dimensionId) {
        // local cache doesn't support cross-dimensional lookups
        if (dimensionId != getActualDimension()) return false;
        return super.isNewChunk(chunkPosX, chunkPosZ);
    }

    @Override
    public List<HighlightAtChunkPos> getNewChunksInRegion(final int leafRegionX, final int leafRegionZ, final int level, final int dimension) {
        if (dimension != getActualDimension()) return Collections.emptyList();
        return super.getNewChunksInRegion(leafRegionX, leafRegionZ, level);
    }

    @Override
    public void handleWorldChange() {

    }

    @Override
    public void handleTick() {

    }

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }
}
