package xaeroplus.feature.render;

import net.minecraft.client.Minecraft;
import xaeroplus.util.FloatSupplier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

public class DrawFeatureRegistry {
    private final HashMap<String, ChunkHighlightDrawFeature> chunkHighlightDrawFeatures = new HashMap<>();
    private final HashMap<String, LineDrawFeature> lineDrawFeatures = new HashMap<>();
    private final List<String> sortedChunkHighlightKeySet = new ArrayList<>();
    private final List<String> sortedLineKeySet = new ArrayList<>();

    public synchronized void registerDirectChunkHighlightProvider(String id, DirectChunkHighlightSupplier chunkHighlightSupplier, IntSupplier colorSupplier) {
        registerChunkHighlightDrawFeature(id, new DirectChunkHighlightDrawFeature(new DirectChunkHighlightProvider(chunkHighlightSupplier, colorSupplier), false));
    }

    // refresh render buffers every tick instead of lazily
    public synchronized void registerDirectChunkHighlightProvider(String id, boolean refreshEveryTick, DirectChunkHighlightSupplier chunkHighlightSupplier, IntSupplier colorSupplier) {
        registerChunkHighlightDrawFeature(id, new DirectChunkHighlightDrawFeature(new DirectChunkHighlightProvider(chunkHighlightSupplier, colorSupplier), refreshEveryTick));
    }

    public synchronized void registerAsyncChunkHighlightProvider(String id, AsyncChunkHighlightSupplier chunkHighlightSupplier, IntSupplier colorSupplier) {
        registerChunkHighlightDrawFeature(id, new AsyncChunkHighlightDrawFeature(new AsyncChunkHighlightProvider(chunkHighlightSupplier, colorSupplier)));
    }

    private synchronized void registerChunkHighlightDrawFeature(String id, ChunkHighlightDrawFeature drawFeature) {
        unregisterChunkHighlightProvider(id); // just in case
        chunkHighlightDrawFeatures.put(id, drawFeature);
        sortedChunkHighlightKeySet.add(id);
        // arbitrary order, just needs to be consistent so colors blend consistently
        sortedChunkHighlightKeySet.sort(Comparator.naturalOrder());
    }

    public synchronized void unregisterChunkHighlightProvider(String id) {
        sortedChunkHighlightKeySet.remove(id);
        ChunkHighlightDrawFeature feature = chunkHighlightDrawFeatures.remove(id);
        if (feature != null) {
            Minecraft.getInstance().execute(feature::close);
        }
    }

    public synchronized void registerLineProvider(String id, LineSupplier lineSupplier, IntSupplier colorSupplier, FloatSupplier lineWidthSupplier, int refreshIntervalMs) {
        unregisterLineProvider(id); // just in case
        lineDrawFeatures.put(id, new LineDrawFeature(new LineProvider(lineSupplier, colorSupplier, lineWidthSupplier), refreshIntervalMs));
        sortedLineKeySet.add(id);
        sortedLineKeySet.sort(Comparator.naturalOrder());
    }

    public synchronized void unregisterLineProvider(String id) {
        sortedLineKeySet.remove(id);
        lineDrawFeatures.remove(id);
    }

    protected synchronized void invalidateCaches() {
        chunkHighlightDrawFeatures.values().forEach(ChunkHighlightDrawFeature::invalidateCache);
        lineDrawFeatures.values().forEach(LineDrawFeature::invalidateCache);
    }

    protected synchronized void forEachChunkHighlightDrawFeature(Consumer<ChunkHighlightDrawFeature> consumer) {
        for (int i = 0; i < sortedChunkHighlightKeySet.size(); i++) {
            var feature = chunkHighlightDrawFeatures.get(sortedChunkHighlightKeySet.get(i));
            if (feature != null) consumer.accept(feature);
        }
    }

    protected synchronized void forEachLineDrawFeature(Consumer<LineDrawFeature> consumer) {
        for (int i = 0; i < sortedLineKeySet.size(); i++) {
            var feature = lineDrawFeatures.get(sortedLineKeySet.get(i));
            if (feature != null) consumer.accept(feature);
        }
    }
}
