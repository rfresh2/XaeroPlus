package xaeroplus.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import xaeroplus.XaeroPlus;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import static xaeroplus.util.ChunkUtils.*;

public class NewChunksLocalCache implements NewChunksCache {
    private final Long2LongOpenHashMap chunks = new Long2LongOpenHashMap();
    private static final int maxNumber = 5000;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    @Override
    public void addNewChunk(final int x, final int z) {
        final long chunkPosKey = chunkPosToLong(x, z);
        try {
            if (chunks.size() > maxNumber) {
                if (lock.readLock().tryLock(1, TimeUnit.SECONDS)) {
                    // remove oldest 500 chunks
                    final List<Long> toRemove = chunks.entrySet().stream()
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
            if (lock.writeLock().tryLock(1, TimeUnit.SECONDS)) {
                chunks.put(chunkPosKey, System.currentTimeMillis());
                lock.writeLock().unlock();
            }

        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error adding new chunk to local cache", e);
        }
    }

    @Override
    public boolean isNewChunk(final int chunkPosX, final int chunkPosZ, final int dimensionId) {
        // local cache doesn't support cross-dimensional lookups
        if (dimensionId != getMCDimension()) return false;
        try {
            if (lock.readLock().tryLock(1, TimeUnit.SECONDS)) {
                boolean containsKey = chunks.containsKey(chunkPosToLong(chunkPosX, chunkPosZ));
                lock.readLock().unlock();
                return containsKey;
            }
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error checking if chunk is new", e);
        }
        return false;
    }

    public boolean isNewChunk(final long chunkPos) {
        try {
            if (lock.readLock().tryLock(1, TimeUnit.SECONDS)) {
                boolean containsKey = chunks.containsKey(chunkPos);
                lock.readLock().unlock();
                return containsKey;
            }
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error checking if chunk is new", e);
        }
        return false;
    }

    private final Cache<RegionRenderPos, List<HighlightAtChunkPos>> regionRenderCache = CacheBuilder.newBuilder()
            .expireAfterWrite(500, TimeUnit.MILLISECONDS)
            .build();

    @Override
    public List<HighlightAtChunkPos> getNewChunksInRegion(final int leafRegionX, final int leafRegionZ, final int level, final int dimension) {
        final RegionRenderPos regionRenderPos = new RegionRenderPos(leafRegionX, leafRegionZ, level);
        try {
            return regionRenderCache.get(regionRenderPos, loadHighlightChunksAtRegion(leafRegionX, leafRegionZ, level, this::isNewChunk));
        } catch (ExecutionException e) {
            XaeroPlus.LOGGER.error("Error handling NewChunks region lookup", e);
        }
        return Collections.emptyList();
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

    @Override
    public Long2LongOpenHashMap getNewChunksState() {
        return chunks;
    }

    @Override
    public void loadPreviousState(final Long2LongOpenHashMap state) {
        chunks.putAll(state);
    }
}
