package xaeroplus.feature.render.text;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import xaeroplus.Globals;
import xaeroplus.module.impl.TickTaskExecutor;
import xaeroplus.util.ChunkUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static xaeroplus.util.GuiMapHelper.*;

public class AsyncTextDrawFeature extends AbstractTextDrawFeature {
    private final TextSupplier textSupplier;
    private final String id;
    private final AsyncLoadingCache<Long, List<Text>> textRenderCache;

    public AsyncTextDrawFeature(String id, TextSupplier textSupplier, final int refreshIntervalMs) {
        this.id = id;
        this.textSupplier = textSupplier;
        this.textRenderCache = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .refreshAfterWrite(refreshIntervalMs, TimeUnit.MILLISECONDS)
            .executor(TickTaskExecutor.INSTANCE)
            .buildAsync(k -> loadTextInWindow());
    }

    private List<Text> loadTextInWindow() {
        List<Text> out = new ArrayList<>();
        var texts = provideTextInWindow();
        var it = Long2ObjectMaps.fastIterator(texts);
        while (it.hasNext()) {
            var text = it.next().getValue();
            out.add(text);
        }
        return out;
    }

    private Long2ObjectMap<Text> provideTextInWindow() {
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
        return textSupplier.getText(windowX, windowZ, windowSize, Globals.getCurrentDimensionId());
    }

    @Override
    public List<Text> getTexts() {
        return textRenderCache.get(0L).getNow(Collections.emptyList());
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public void invalidateCache() {
        this.textRenderCache.synchronous().invalidateAll();
    }

    @Override
    public void close() {
        this.textRenderCache.synchronous().invalidateAll();
    }
}
