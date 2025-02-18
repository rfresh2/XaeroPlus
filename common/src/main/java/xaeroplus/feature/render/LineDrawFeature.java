package xaeroplus.feature.render;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import xaeroplus.Globals;
import xaeroplus.module.ModuleManager;
import xaeroplus.module.impl.TickTaskExecutor;
import xaeroplus.util.ChunkUtils;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static xaeroplus.util.GuiMapHelper.*;

public class LineDrawFeature {
    private final AsyncLoadingCache<Long, List<Line>> lineRenderCache;
    private final LineProvider lineProvider;

    public LineDrawFeature(LineProvider lineProvider, int refreshIntervalMs) {
        this.lineProvider = lineProvider;
        this.lineRenderCache = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .refreshAfterWrite(refreshIntervalMs, TimeUnit.MILLISECONDS)
            .executor(ModuleManager.getModule(TickTaskExecutor.class))
            .buildAsync(k -> loadLinesInWindow());
    }

    private List<Line> loadLinesInWindow() {
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
        return lineProvider.lineSupplier().getLines(windowX, windowZ, windowSize, Globals.getCurrentDimensionId());
    }

    public int colorInt() {
        return lineProvider.colorSupplier().getAsInt();
    }

    public float lineWidth() {
        return lineProvider.lineWidthSupplier().getFloat();
    }

    public void invalidateCache() {
        lineRenderCache.synchronous().invalidateAll();
    }

    public List<Line> getLines() {
        return lineRenderCache.get(0L).getNow(Collections.emptyList());
    }
}
