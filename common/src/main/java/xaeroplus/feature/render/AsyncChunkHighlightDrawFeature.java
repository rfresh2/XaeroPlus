package xaeroplus.feature.render;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongMaps;
import xaeroplus.Globals;
import xaeroplus.util.ChunkUtils;

import java.util.concurrent.TimeUnit;

import static xaeroplus.util.GuiMapHelper.*;

public class AsyncChunkHighlightDrawFeature implements ChunkHighlightDrawFeature {
    private final AsyncLoadingCache<Long, Long2LongMap> chunkRenderCache;
    private final AsyncChunkHighlightProvider chunkHighlightProvider;
    private final HighlightDrawBuffer drawBuffer = new HighlightDrawBuffer();

    public AsyncChunkHighlightDrawFeature(AsyncChunkHighlightProvider chunkHighlightProvider) {
        this.chunkHighlightProvider = chunkHighlightProvider;
        this.chunkRenderCache = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .refreshAfterWrite(500, TimeUnit.MILLISECONDS)
            .executor(Globals.cacheRefreshExecutorService.get())
            .removalListener((k, v, cause) -> markDrawBuffersStale())
            // only one key
            .buildAsync(k -> loadFeatureHighlightsInWindow());
    }

    private Long2LongMap loadFeatureHighlightsInWindow() {
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

    @Override
    public int colorInt() {
        return chunkHighlightProvider.colorSupplier().getAsInt();
    }

    @Override
    public void invalidateCache() {
        chunkRenderCache.synchronous().invalidateAll();
    }

    @Override
    public Long2LongMap getChunkHighlights() {
        return chunkRenderCache.get(0L).getNow(Long2LongMaps.EMPTY_MAP);
    }

    @Override
    public void render(final boolean worldmap) {
        Long2LongMap highlights = getChunkHighlights(); // needed for cache to async refresh
        if (drawBuffer.needsRefresh(worldmap)) {
            drawBuffer.refresh(highlights, worldmap);
        }
        drawBuffer.render();
    }

    public void markDrawBuffersStale() {
        drawBuffer.markStale();
    }

    @Override
    public void close() {
        drawBuffer.close();
    }
}
