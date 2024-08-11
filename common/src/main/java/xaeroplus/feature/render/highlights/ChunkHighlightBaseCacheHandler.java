package xaeroplus.feature.render.highlights;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaeroplus.XaeroPlus;
import xaeroplus.util.ChunkUtils;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.StampedLock;

import static xaeroplus.util.ChunkUtils.chunkPosToLong;

public abstract class ChunkHighlightBaseCacheHandler implements ChunkHighlightCache {
    public final ReadWriteLock lock = new StampedLock().asReadWriteLock();
    public final Long2LongMap chunks = new Long2LongOpenHashMap();

    @Override
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
            XaeroPlus.LOGGER.error("Failed to add new highlight: {}, {}", x, z, e);
        }
        return true;
    }

    @Override
    public boolean removeHighlight(final int x, final int z) {
        final long chunkPos = chunkPosToLong(x, z);
        try {
            if (lock.writeLock().tryLock(1, TimeUnit.SECONDS)) {
                chunks.remove(chunkPos);
                lock.writeLock().unlock();
            }
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Failed to add new highlight: {}, {}", x, z, e);
        }
        return true;
    }

    @Override
    public boolean isHighlighted(final int x, final int z, ResourceKey<Level> dimensionId) {
        return isHighlighted(chunkPosToLong(x, z));
    }

    @Override
    public LongSet getWindowedHighlightsSnapshot(final int windowRegionX, final int windowRegionZ, final int windowRegionSize, final ResourceKey<Level> dimension) {
        try {
            if (lock.readLock().tryLock(1, TimeUnit.SECONDS)) {
                // ignoring window as its probably faster than iterating over all currently loaded highlights lol
                // also when we use the saving cache we already resize the cache's window, so only could have mem reduction on local caches

                // copy is memory inefficient but we need a thread safe iterator for rendering
                var set = new LongOpenHashSet(chunks.keySet());
                lock.readLock().unlock();
                return set;
            }
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error getting windowed highlights: {}, {}, {}", windowRegionX, windowRegionZ, windowRegionSize, e);
        }
        return LongSet.of();
    }

    public boolean isHighlighted(final long chunkPos) {
        try {
            return chunks.containsKey(chunkPos);
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error checking if chunk is highlighted: {}, {}", ChunkUtils.longToChunkX(chunkPos), ChunkUtils.longToChunkZ(chunkPos), e);
        }
        return false;
    }

    @Override
    public Long2LongMap getHighlightsState() {
        return chunks;
    }

    @Override
    public void loadPreviousState(final Long2LongMap state) {
        if (state == null) return;
        try {
            if (lock.writeLock().tryLock(1, TimeUnit.SECONDS)) {
                chunks.putAll(state);
                lock.writeLock().unlock();
            }
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error loading previous highlight cache state", e);
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
            XaeroPlus.LOGGER.error("Failed replacing highlight cache state", e);
        }
    }

    public void reset() {
        try {
            if (lock.writeLock().tryLock(1, TimeUnit.SECONDS)) {
                chunks.clear();
                lock.writeLock().unlock();
            }
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Failed resetting highlight cache", e);
        }
    }
}
