package xaeroplus.util.highlights;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import xaeroplus.XaeroPlus;
import xaeroplus.util.RegionRenderPos;
import xaeroplus.util.Shared;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.StampedLock;

import static xaeroplus.util.ChunkUtils.chunkPosToLong;
import static xaeroplus.util.ChunkUtils.loadHighlightChunksAtRegion;

public abstract class ChunkHighlightBaseCacheHandler implements ChunkHighlightCache {
    public final ReadWriteLock lock = new StampedLock().asReadWriteLock();
    public final Long2LongMap chunks = new Long2LongOpenHashMap();
    private final AsyncLoadingCache<RegionRenderPos, List<HighlightAtChunkPos>> regionRenderCache = Caffeine.newBuilder()
            .expireAfterWrite(3000, TimeUnit.MILLISECONDS)
            .refreshAfterWrite(500, TimeUnit.MILLISECONDS)
            .executor(Shared.cacheRefreshExecutorService)
            .buildAsync(key -> loadHighlightChunksAtRegion(key.leafRegionX, key.leafRegionZ, key.level, this::isHighlightedWithWait).call());

    public boolean addHighlight(final int x, final int z) {
        return addHighlight(x, z, System.currentTimeMillis());
    }

    public boolean addHighlight(final int x, final int z, final long foundTime) {
        final long chunkPos = chunkPosToLong(x, z);
        try {
            if (lock.writeLock().tryLock(1, TimeUnit.SECONDS)) {
                chunks.put(chunkPos, foundTime);
                lock.writeLock().unlock();
            }
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Failed to add new chunk", e);
        }
        return true;
    }

    public boolean isHighlighted(final int x, final int z, RegistryKey<World> dimensionId) {
        return isHighlighted(chunkPosToLong(x, z));
    }

    public boolean isHighlighted(final long chunkPos) {
        try {
            if (lock.readLock().tryLock()) {
                boolean containsKey = chunks.containsKey(chunkPos);
                lock.readLock().unlock();
                return containsKey;
            }
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error checking if chunk contains portal", e);
        }
        return false;
    }

    public boolean isHighlightedWithWait(final long chunkPos) {
        try {
            if (lock.readLock().tryLock(1, TimeUnit.SECONDS)) {
                boolean containsKey = chunks.containsKey(chunkPos);
                lock.readLock().unlock();
                return containsKey;
            }
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error checking if chunk contains portal", e);
        }
        return false;
    }

    public List<HighlightAtChunkPos> getHighlightsInRegion(final int leafRegionX, final int leafRegionZ, final int level, RegistryKey<World> dimension) {
        try {
            final CompletableFuture<List<HighlightAtChunkPos>> future = regionRenderCache.get(new RegionRenderPos(leafRegionX, leafRegionZ, level));
            if (future.isDone()) {
                return future.get();
            } else {
                return Collections.emptyList();
            }
        } catch (Exception e) {
            XaeroPlus.LOGGER.error("Error handling Portals region lookup", e);
        }
        return Collections.emptyList();
    }

    public Long2LongMap getHighlightsState() {
        return chunks;
    }

    public void loadPreviousState(final Long2LongMap state) {
        try {
            if (lock.writeLock().tryLock(1, TimeUnit.SECONDS)) {
                chunks.putAll(state);
                lock.writeLock().unlock();
            }
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error loading previous state", e);
        }
    }

    public void removeHighlight(final int x, final int z) {
        try {
            if (lock.writeLock().tryLock(1, TimeUnit.SECONDS)) {
                chunks.remove(chunkPosToLong(x, z));
                lock.writeLock().unlock();
            }
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error removing highlight", e);
        }
    }

    public void replaceState(final Long2LongOpenHashMap state) {
        try {
            if (lock.writeLock().tryLock(1, TimeUnit.SECONDS)) {
                this.chunks.clear();
                this.chunks.putAll(state);
                lock.writeLock().unlock();
            }
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Failed replacing cache state", e);
        }
    }

    public void reset() {
        try {
            if (lock.writeLock().tryLock(1, TimeUnit.SECONDS)) {
                chunks.clear();
                lock.writeLock().unlock();
            }
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Failed resetting cache", e);
        }
    }
}
