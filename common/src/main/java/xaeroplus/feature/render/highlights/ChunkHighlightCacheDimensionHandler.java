package xaeroplus.feature.render.highlights;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import xaeroplus.util.ChunkUtils;

import java.util.ArrayList;
import java.util.List;

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
        boolean windowChanged = regionX != windowRegionX || regionZ != windowRegionZ || regionSize != windowRegionSize;
        this.windowRegionX = regionX;
        this.windowRegionZ = regionZ;
        this.windowRegionSize = regionSize;
        if (windowChanged) {
            writeHighlightsOutsideWindowToDatabase();
            loadHighlightsInWindow();
        }
    }

    private void loadHighlightsInWindow() {
        executorService.execute(() -> {
            synchronized (this.chunks) {
                database.getHighlightsInWindow(
                    dimension,
                    windowRegionX - windowRegionSize, windowRegionX + windowRegionSize,
                    windowRegionZ - windowRegionSize, windowRegionZ + windowRegionSize,
                    (x, y, time) -> this.chunks.put(chunkPosToLong(x, y), time)
                );
            }
        });
    }

    private void writeHighlightsOutsideWindowToDatabase() {
        executorService.execute(() -> {
            final List<ChunkHighlightData> chunksToWrite = new ArrayList<>();
            var minChunkX = regionCoordToChunkCoord(windowRegionX - windowRegionSize);
            var maxChunkX = regionCoordToChunkCoord(windowRegionX + windowRegionSize);
            var minChunkZ = regionCoordToChunkCoord(windowRegionZ - windowRegionSize);
            var maxChunkZ = regionCoordToChunkCoord(windowRegionZ + windowRegionSize);
            synchronized (this.chunks) {
                for (var it = chunks.long2LongEntrySet().iterator(); it.hasNext(); ) {
                    var entry = it.next();
                    final long chunkPos = entry.getLongKey();
                    final int chunkX = ChunkUtils.longToChunkX(chunkPos);
                    final int chunkZ = ChunkUtils.longToChunkZ(chunkPos);
                    if (chunkX < minChunkX
                        || chunkX > maxChunkX
                        || chunkZ < minChunkZ
                        || chunkZ > maxChunkZ) {
                        chunksToWrite.add(new ChunkHighlightData(chunkX, chunkZ, entry.getLongValue()));
                        it.remove();
                    }
                }
            }
            database.insertHighlightList(chunksToWrite, dimension);
        });
    }

    public ListenableFuture<?> writeAllHighlightsToDatabase() {
        return executorService.submit(() -> {
            final List<ChunkHighlightData> chunksToWrite = new ArrayList<>(chunks.size());
            synchronized (chunks) {
                for (var it = chunks.long2LongEntrySet().iterator(); it.hasNext(); ) {
                    var entry = it.next();
                    final long chunkPos = entry.getLongKey();
                    final int chunkX = ChunkUtils.longToChunkX(chunkPos);
                    final int chunkZ = ChunkUtils.longToChunkZ(chunkPos);
                    chunksToWrite.add(new ChunkHighlightData(chunkX, chunkZ, entry.getLongValue()));
                }
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
