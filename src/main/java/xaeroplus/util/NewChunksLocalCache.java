package xaeroplus.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import xaeroplus.XaeroPlus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static xaeroplus.util.ChunkUtils.*;

public class NewChunksLocalCache {
    private final Long2LongOpenHashMap chunks = new Long2LongOpenHashMap();
    private final int dimension;
    private int windowRegionX = 0;
    private int windowRegionZ = 0;
    // square centered at windowX, windowZ with size windowSize
    private int windowRegionSize = 0;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final NewChunksDatabase database;

    private final Cache<RegionRenderPos, List<HighlightAtChunkPos>> regionRenderCache = CacheBuilder.newBuilder()
            .expireAfterWrite(500, TimeUnit.MILLISECONDS)
            .build();

    public NewChunksLocalCache(final int dimension, final NewChunksDatabase database) {
        this.dimension = dimension;
        this.database = database;
    }

    public void setWindow(int regionX, int regionZ, int regionSize) {
        this.windowRegionX = regionX;
        this.windowRegionZ = regionZ;
        this.windowRegionSize = regionSize;
        writeNewChunksOutsideWindowToDatabase();
        loadNewChunksInWindow();
    }

    public void addNewChunk(final int x, final int z, final long foundTime) {
        final long chunkPos = chunkPosToLong(x, z);
        try {
            lock.writeLock().lock();
            chunks.put(chunkPos, foundTime);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void addNewChunk(final int x, final int z) {
        addNewChunk(x, z, System.currentTimeMillis());
    }

    private void loadNewChunksInWindow() {
        final List<NewChunkData> newChunks = database.getNewChunksInWindow(dimension,
                windowRegionX - windowRegionSize, windowRegionX + windowRegionSize,
                windowRegionZ - windowRegionSize, windowRegionZ + windowRegionSize
                );
        try {
            lock.writeLock().lock();
            for (final NewChunkData chunk : newChunks) {
                chunks.put(chunkPosToLong(chunk.x, chunk.z), chunk.foundTime);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void writeNewChunksOutsideWindowToDatabase() {
        final List<NewChunkData> chunksToWrite = new ArrayList<>();
        try {
            lock.writeLock().lock();
            chunks.long2LongEntrySet().removeIf(entry -> {
                final long chunkPos = entry.getLongKey();
                final int chunkX = ChunkUtils.longToChunkX(chunkPos);
                final int chunkZ = ChunkUtils.longToChunkZ(chunkPos);
                if (chunkX < regionCoordToChunkCoord(windowRegionX - windowRegionSize)
                        || chunkX > regionCoordToChunkCoord(windowRegionX + windowRegionSize)
                        || chunkZ < regionCoordToChunkCoord(windowRegionZ - windowRegionSize)
                        || chunkZ > regionCoordToChunkCoord(windowRegionZ + windowRegionSize)) {
                    chunksToWrite.add(new NewChunkData(chunkX, chunkZ, entry.getLongValue()));
                    return true;
                }
                return false;
            });
        } finally {
            lock.writeLock().unlock();
        }
        database.insertNewChunkList(chunksToWrite, dimension);
    }

    public void writeAllChunksToDatabase() {
        final List<NewChunkData> chunksToWrite = new ArrayList<>();
        try {
            lock.readLock().lock();
            chunks.long2LongEntrySet().forEach(entry -> {
                final long chunkPos = entry.getLongKey();
                final int chunkX = ChunkUtils.longToChunkX(chunkPos);
                final int chunkZ = ChunkUtils.longToChunkZ(chunkPos);
                chunksToWrite.add(new NewChunkData(chunkX, chunkZ, entry.getLongValue()));
            });
        } finally {
            lock.readLock().unlock();
        }
        database.insertNewChunkList(chunksToWrite, dimension);
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

    public List<HighlightAtChunkPos> getNewChunksInRegion(final int leafRegionX, final int leafRegionZ, final int level) {
        final RegionRenderPos regionRenderPos = new RegionRenderPos(leafRegionX, leafRegionZ, level);
        try {
            return regionRenderCache.get(regionRenderPos, loadHighlightChunksAtRegion(leafRegionX, leafRegionZ, level, this::isNewChunk));
        } catch (ExecutionException e) {
            XaeroPlus.LOGGER.error("Error handling NewChunks region lookup", e);
        }
        return Collections.emptyList();
    }
}
