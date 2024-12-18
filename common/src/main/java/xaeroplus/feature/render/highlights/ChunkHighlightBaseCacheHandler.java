package xaeroplus.feature.render.highlights;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
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
    public LongList getHighlightsSnapshot(final ResourceKey<Level> dimension) {
        try {
            if (lock.readLock().tryLock(1, TimeUnit.SECONDS)) {
                // copy is memory inefficient but we need a thread safe iterator for rendering
                var list = new LongArrayList(chunks.keySet());
                lock.readLock().unlock();
                return list;
            }
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error getting highlights snapshot in dimension: {}", dimension.location().getPath(), e);
        }
        return LongList.of();
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
