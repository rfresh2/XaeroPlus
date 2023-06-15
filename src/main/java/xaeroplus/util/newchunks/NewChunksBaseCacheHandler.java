package xaeroplus.util.newchunks;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import xaeroplus.XaeroPlus;
import xaeroplus.util.HighlightAtChunkPos;
import xaeroplus.util.RegionRenderPos;
import xaeroplus.util.Shared;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static xaeroplus.util.ChunkUtils.chunkPosToLong;
import static xaeroplus.util.ChunkUtils.loadHighlightChunksAtRegion;

public abstract class NewChunksBaseCacheHandler {
    final Long2LongOpenHashMap chunks = new Long2LongOpenHashMap();
    final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final AsyncLoadingCache<RegionRenderPos, List<HighlightAtChunkPos>> regionRenderCache = Caffeine.newBuilder()
            .expireAfterWrite(3000, TimeUnit.MILLISECONDS)
            .refreshAfterWrite(500, TimeUnit.MILLISECONDS)
            .executor(Shared.cacheRefreshExecutorService)
            .buildAsync(key -> loadHighlightChunksAtRegion(key.leafRegionX, key.leafRegionZ, key.level, this::isNewChunk).call());

    public void addNewChunk(final int x, final int z, final long foundTime) {
        final long chunkPos = chunkPosToLong(x, z);
        try {
            if (lock.writeLock().tryLock(1, TimeUnit.SECONDS)) {
                chunks.put(chunkPos, foundTime);
                lock.writeLock().unlock();
            }
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Failed to add new chunk", e);
        }
    }

    public void addNewChunk(final int x, final int z) {
        addNewChunk(x, z, System.currentTimeMillis());
    }

    // in chunkpos coordinates
    public boolean isNewChunk(final int x, final int z) {
        return isNewChunk(chunkPosToLong(x, z));
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

    public Long2LongOpenHashMap getNewChunksState() {
        return chunks;
    }

    public void loadPreviousState(final Long2LongOpenHashMap state) {
        try {
            if (lock.writeLock().tryLock(1, TimeUnit.SECONDS)) {
                chunks.putAll(state);
                lock.writeLock().unlock();
            }
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error loading previous state", e);
        }
    }

    public List<HighlightAtChunkPos> getNewChunksInRegion(final int leafRegionX, final int leafRegionZ, final int level) {
        try {
            final CompletableFuture<List<HighlightAtChunkPos>> future = regionRenderCache.get(new RegionRenderPos(leafRegionX, leafRegionZ, level));
            if (future.isDone()) {
                return future.get();
            } else {
                return Collections.emptyList();
            }
        } catch (Exception e) {
            XaeroPlus.LOGGER.error("Error handling NewChunks region lookup", e);
        }
        return Collections.emptyList();
    }
}
