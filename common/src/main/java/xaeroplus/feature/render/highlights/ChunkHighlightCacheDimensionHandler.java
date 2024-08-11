package xaeroplus.feature.render.highlights;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import xaeroplus.XaeroPlus;
import xaeroplus.util.ChunkUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static xaeroplus.util.ChunkUtils.chunkPosToLong;
import static xaeroplus.util.ChunkUtils.regionCoordToChunkCoord;

public class ChunkHighlightCacheDimensionHandler extends ChunkHighlightBaseCacheHandler {
    @NotNull private final ResourceKey<Level> dimension;
    private int windowRegionX = 0;
    private int windowRegionZ = 0;
    // square centered at windowX, windowZ with size windowSize
    private int windowRegionSize = 0;
    @NotNull private final ChunkHighlightDatabase database;
    @NotNull private final ListeningExecutorService executorService;

    public ChunkHighlightCacheDimensionHandler(
        @NotNull ResourceKey<Level> dimension,
        @NotNull ChunkHighlightDatabase database,
        @NotNull ListeningExecutorService executorService) {
        this.dimension = dimension;
        this.database = database;
        this.executorService = executorService;
    }

    public void setWindow(int regionX, int regionZ, int regionSize) {
        this.windowRegionX = regionX;
        this.windowRegionZ = regionZ;
        this.windowRegionSize = regionSize;
        writeHighlightsOutsideWindowToDatabase();
        loadHighlightsInWindow();
    }

    private void loadHighlightsInWindow() {
        executorService.execute(() -> {
            final List<ChunkHighlightData> chunks = database.getHighlightsInWindow(
                    dimension,
                    windowRegionX - windowRegionSize, windowRegionX + windowRegionSize,
                    windowRegionZ - windowRegionSize, windowRegionZ + windowRegionSize
            );
            try {
                if (lock.writeLock().tryLock(1, TimeUnit.SECONDS)) {
                    for (final ChunkHighlightData chunk : chunks) {
                        this.chunks.put(chunkPosToLong(chunk.x(), chunk.z()), chunk.foundTime());
                    }
                    lock.writeLock().unlock();
                }
            } catch (final Exception e) {
                XaeroPlus.LOGGER.error("Failed to load highlights in window for {} disk cache dimension: {}", database.databaseName, dimension.location(), e);
            }
        });
    }

    private void writeHighlightsOutsideWindowToDatabase() {
        executorService.execute(() -> {
            final List<ChunkHighlightData> chunksToWrite = new ArrayList<>();
            try {
                if (lock.writeLock().tryLock(1L, TimeUnit.SECONDS)) {
                    chunks.long2LongEntrySet().removeIf(entry -> {
                        final long chunkPos = entry.getLongKey();
                        final int chunkX = ChunkUtils.longToChunkX(chunkPos);
                        final int chunkZ = ChunkUtils.longToChunkZ(chunkPos);
                        if (chunkX < regionCoordToChunkCoord(windowRegionX - windowRegionSize)
                                || chunkX > regionCoordToChunkCoord(windowRegionX + windowRegionSize)
                                || chunkZ < regionCoordToChunkCoord(windowRegionZ - windowRegionSize)
                                || chunkZ > regionCoordToChunkCoord(windowRegionZ + windowRegionSize)) {
                            chunksToWrite.add(new ChunkHighlightData(chunkX, chunkZ, entry.getLongValue()));
                            return true;
                        }
                        return false;
                    });
                    lock.writeLock().unlock();
                }
            } catch (final Exception e) {
                XaeroPlus.LOGGER.error("Error while writing highlights outside window to {} disk cache dimension: {}", database.databaseName, dimension.location(), e);
            }
            database.insertHighlightList(chunksToWrite, dimension);
        });
    }

    public ListenableFuture<?> writeAllHighlightsToDatabase() {
        return executorService.submit(() -> {
            final List<ChunkHighlightData> chunksToWrite = new ArrayList<>(chunks.size());
            try {
                if (lock.readLock().tryLock(1, TimeUnit.SECONDS)) {
                    chunks.long2LongEntrySet().forEach(entry -> {
                        final long chunkPos = entry.getLongKey();
                        final int chunkX = ChunkUtils.longToChunkX(chunkPos);
                        final int chunkZ = ChunkUtils.longToChunkZ(chunkPos);
                        chunksToWrite.add(new ChunkHighlightData(chunkX, chunkZ, entry.getLongValue()));
                    });
                    lock.readLock().unlock();
                }
            } catch (final Exception e) {
                XaeroPlus.LOGGER.error("Error while writing all chunks to {} disk cache dimension: {}", database.databaseName, dimension.location(), e);
            }
            database.insertHighlightList(chunksToWrite, dimension);
        });
    }

    @Override
    public boolean removeHighlight(final int x, final int z) {
        super.removeHighlight(x, z);
        database.removeHighlight(x, z, dimension);
        return true;
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
