package xaeroplus.feature.render.highlight;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongMaps;
import xaeroplus.Globals;
import xaeroplus.feature.render.DrawContext;
import xaeroplus.feature.render.MapRenderWindow;

import java.util.concurrent.TimeUnit;

public class AsyncChunkHighlightDrawFeature extends AbstractChunkHighlightDrawFeature {
    private final String id;
    private final AsyncLoadingCache<Long, Long2LongMap> chunkRenderCache;
    private final AsyncChunkHighlightProvider chunkHighlightProvider;

    public AsyncChunkHighlightDrawFeature(String id, AbstractHighlightVertexBuffer drawBuffer, AsyncChunkHighlightProvider chunkHighlightProvider) {
        super(drawBuffer);
        this.id = id;
        this.chunkHighlightProvider = chunkHighlightProvider;
        this.chunkRenderCache = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .refreshAfterWrite(500, TimeUnit.MILLISECONDS)
            .executor(Globals.cacheRefreshExecutorService.get())
            .removalListener((k, v, cause) -> drawBuffer.markStale())
            // only one key
            .buildAsync(k -> loadFeatureHighlightsInWindow());
    }

    private Long2LongMap loadFeatureHighlightsInWindow() {
        var window = MapRenderWindow.resolveCurrent();
        return chunkHighlightProvider.chunkHighlightSupplier()
            .getHighlights(window.windowX(), window.windowZ(), window.windowSize(), window.dimension());
    }

    @Override
    public String id() {
        return id;
    }

    public Long2LongMap chunkHighlights() {
        return chunkRenderCache.get(0L).getNow(Long2LongMaps.EMPTY_MAP);
    }

    public int color() {
        return chunkHighlightProvider.colorSupplier().getAsInt();
    }

    @Override
    public void preRender(final DrawContext ctx) {
        super.preRender(ctx);
        drawBuffer.preRender(ctx, chunkHighlights(), color());
    }

    @Override
    public void render(final DrawContext ctx) {
        preRender(ctx);
        drawBuffer.render();
        postRender(ctx);
    }
}
