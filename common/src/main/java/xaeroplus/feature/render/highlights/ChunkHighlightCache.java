package xaeroplus.feature.render.highlights;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaeroplus.event.XaeroWorldChangeEvent;
import xaeroplus.module.ModuleManager;
import xaeroplus.module.impl.TickTaskExecutor;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public interface ChunkHighlightCache {
    void addHighlight(final int x, final int z);
    void removeHighlight(final int x, final int z);
    boolean isHighlighted(final int x, final int z, ResourceKey<Level> dimensionId);
    /**
     * retrieves the current cache map for the given dimension. Database data is loaded in a window around the current view asynchronously
     *
     * Do not do any operations on this map off the main mc thread, its a direct reference to the cache map
     */
    Long2LongMap getCacheMap(ResourceKey<Level> dimensionId);
    /**
     * Gets all highlight data both from the database and local cache in a square set of regions.
     * Can be used to get highlight data that may be outside the current loaded window
     *
     * @param dimension the dimension to get highlights for
     * @param windowRegionX Centered region X coordinate
     * @param windowRegionZ Centered region Z coordinate
     * @param windowRegionSize Region window size.
     *                   Total area of square = (2 * (windowRegionSize + 1)) ^ 2
     *                   region = 1024 (32x32) chunks
     * @return Completable future of a Long2LongMap: packed chunk coordinates -> found time unix epoch
     *       To convert packed coordinates in the map, see `ChunkUtils.longToChunkX` and `ChunkUtils.longToChunkZ`
     *       This method is asynchronous, do not block waiting for its result on the main mc thread
     */
    CompletableFuture<Long2LongMap> getHighlightsInCustomWindow(int windowRegionX, int windowRegionZ, int windowRegionSize, ResourceKey<Level> dimension);
    void handleWorldChange(final XaeroWorldChangeEvent event);
    void handleTick();
    void onEnable();
    void onDisable();
    default <V> CompletableFuture<V> submitTickTask(final Supplier<V> task) {
        return ModuleManager.getModule(TickTaskExecutor.class).submit(task);
    }
    default CompletableFuture<Void> submitTickTask(final Runnable task) {
        return ModuleManager.getModule(TickTaskExecutor.class).submit(task);
    }
}
