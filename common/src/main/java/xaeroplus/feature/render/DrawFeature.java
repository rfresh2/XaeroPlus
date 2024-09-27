package xaeroplus.feature.render;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import it.unimi.dsi.fastutil.longs.LongList;
import xaeroplus.Globals;
import xaeroplus.util.ChunkUtils;

import java.util.concurrent.TimeUnit;

import static xaeroplus.util.GuiMapHelper.*;

public class DrawFeature {
    private final AsyncLoadingCache<Long, LongList> chunkRenderCache;
    private final ChunkHighlightProvider chunkHighlightProvider;
    private final HighlightDrawBuffer drawBuffer = new HighlightDrawBuffer();

    public DrawFeature(ChunkHighlightProvider chunkHighlightProvider) {
        this.chunkHighlightProvider = chunkHighlightProvider;
        this.chunkRenderCache = createChunkHighlightRenderCache(chunkHighlightProvider);
    }

    private AsyncLoadingCache<Long, LongList> createChunkHighlightRenderCache(final ChunkHighlightProvider chunkHighlightProvider) {
        return Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .refreshAfterWrite(500, TimeUnit.MILLISECONDS)
            .executor(Globals.cacheRefreshExecutorService.get())
            .removalListener((k, v, cause) -> markDrawBuffersStale())
            // only one key
            .buildAsync(k -> loadFeatureHighlightsInWindow(chunkHighlightProvider));
    }

    private LongList loadFeatureHighlightsInWindow(final ChunkHighlightProvider chunkHighlightProvider) {
        final int windowX, windowZ, windowSize;
        var guiMapOptional = getGuiMap();
        if (guiMapOptional.isPresent()) {
            var guiMap = guiMapOptional.get();
            windowX = getGuiMapCenterRegionX(guiMap);
            windowZ = getGuiMapCenterRegionZ(guiMap);
            windowSize = getGuiMapRegionSize(guiMap);
        } else {
            windowX = ChunkUtils.getPlayerRegionX();
            windowZ = ChunkUtils.getPlayerRegionZ();
            windowSize = Math.max(3, Globals.minimapScaleMultiplier);
        }
        return chunkHighlightProvider.chunkHighlightSupplier().getHighlights(windowX, windowZ, windowSize, Globals.getCurrentDimensionId());
    }

    public int colorInt() {
        return chunkHighlightProvider.colorSupplier().getAsInt();
    }

    public void invalidateCache() {
        chunkRenderCache.synchronous().invalidateAll();
    }

    public LongList getChunkHighlights() {
        return chunkRenderCache.get(0L).getNow(LongList.of());
    }

    public HighlightDrawBuffer getDrawBuffer() {
        return this.drawBuffer;
    }

    public void markDrawBuffersStale() {
        drawBuffer.markStale();
    }

    public void closeDrawBuffers() {
        drawBuffer.close();
    }
}
