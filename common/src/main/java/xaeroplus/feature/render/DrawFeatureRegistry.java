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
    private final HashMap<String, MultiColorChunkHighlightDrawFeature> multiColorChunkHighlightDrawFeatures = new HashMap<>();
    private final HashMap<String, LineDrawFeature> lineDrawFeatures = new HashMap<>();
    private final HashMap<String, MultiColorLineDrawFeature> multiColorLineDrawFeatures = new HashMap<>();
    private final List<String> sortedChunkHighlightKeySet = new ArrayList<>();
    private final List<String> sortedMultiColorChunkHighlightKeySet = new ArrayList<>();
    private final List<String> sortedLineKeySet = new ArrayList<>();
    private final List<String> sortedMultiColorLineKeySet = new ArrayList<>();

    public synchronized void registerDirectChunkHighlightProvider(String id, DirectChunkHighlightSupplier chunkHighlightSupplier, IntSupplier colorSupplier) {
        registerChunkHighlightDrawFeature(id, new DirectChunkHighlightDrawFeature(new DirectChunkHighlightProvider(chunkHighlightSupplier, colorSupplier), false));
    }

    // refresh render buffers every tick instead of lazily
    public synchronized void registerDirectChunkHighlightProvider(String id, boolean refreshEveryTick, DirectChunkHighlightSupplier chunkHighlightSupplier, IntSupplier colorSupplier) {
        registerChunkHighlightDrawFeature(id, new DirectChunkHighlightDrawFeature(new DirectChunkHighlightProvider(chunkHighlightSupplier, colorSupplier), refreshEveryTick));
    }

    public synchronized void registerDirectMultiColorChunkHighlightProvider(String id, boolean refreshEveryTick, DirectChunkHighlightSupplier chunkHighlightSupplier, IntSupplier colorAlphaSupplier) {
        registerMultiColorChunkHighlightDrawFeature(id, new MultiColorDirectChunkHighlightDrawFeature(new DirectChunkHighlightProvider(chunkHighlightSupplier, colorAlphaSupplier), refreshEveryTick));
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

    private synchronized void registerMultiColorChunkHighlightDrawFeature(String id, MultiColorChunkHighlightDrawFeature drawFeature) {
        unregisterChunkHighlightProvider(id); // just in case
        multiColorChunkHighlightDrawFeatures.put(id, drawFeature);
        sortedMultiColorChunkHighlightKeySet.add(id);
        // arbitrary order, just needs to be consistent so colors blend consistently
        sortedMultiColorChunkHighlightKeySet.sort(Comparator.naturalOrder());
    }

    public synchronized void unregisterChunkHighlightProvider(String id) {
        sortedChunkHighlightKeySet.remove(id);
        ChunkHighlightDrawFeature feature = chunkHighlightDrawFeatures.remove(id);
        if (feature != null) {
            Minecraft.getInstance().execute(feature::close);
        }
    }

    public synchronized void unregisterMultiColorChunkHighlightProvider(String id) {
        sortedMultiColorChunkHighlightKeySet.remove(id);
        MultiColorChunkHighlightDrawFeature feature = multiColorChunkHighlightDrawFeatures.remove(id);
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

    public synchronized void registerMultiColorLineProvider(String id, MultiColorLineSupplier lineSupplier, IntSupplier colorAlphaSupplier, FloatSupplier lineWidthSupplier, int refreshIntervalMs) {
        unregisterMultiColorLineProvider(id); // just in case
        multiColorLineDrawFeatures.put(id, new MultiColorLineDrawFeature(new MultiColorLineProvider(lineSupplier, colorAlphaSupplier, lineWidthSupplier), refreshIntervalMs));
        sortedMultiColorLineKeySet.add(id);
        sortedMultiColorLineKeySet.sort(Comparator.naturalOrder());
    }

    public synchronized void unregisterMultiColorLineProvider(String id) {
        sortedMultiColorLineKeySet.remove(id);
        multiColorLineDrawFeatures.remove(id);
    }

    protected synchronized void invalidateCaches() {
        chunkHighlightDrawFeatures.values().forEach(ChunkHighlightDrawFeature::invalidateCache);
        multiColorChunkHighlightDrawFeatures.values().forEach(MultiColorChunkHighlightDrawFeature::invalidateCache);
        lineDrawFeatures.values().forEach(LineDrawFeature::invalidateCache);
        multiColorLineDrawFeatures.values().forEach(MultiColorLineDrawFeature::invalidateCache);
    }

    protected synchronized void forEachChunkHighlightDrawFeature(Consumer<ChunkHighlightDrawFeature> consumer) {
        for (int i = 0; i < sortedChunkHighlightKeySet.size(); i++) {
            var feature = chunkHighlightDrawFeatures.get(sortedChunkHighlightKeySet.get(i));
            if (feature != null) consumer.accept(feature);
        }
    }

    protected synchronized void forEachMultiColorChunkHighlightDrawFeature(Consumer<MultiColorChunkHighlightDrawFeature> consumer) {
        for (int i = 0; i < sortedMultiColorChunkHighlightKeySet.size(); i++) {
            var feature = multiColorChunkHighlightDrawFeatures.get(sortedMultiColorChunkHighlightKeySet.get(i));
            if (feature != null) consumer.accept(feature);
        }
    }

    protected synchronized void forEachLineDrawFeature(Consumer<LineDrawFeature> consumer) {
        for (int i = 0; i < sortedLineKeySet.size(); i++) {
            var feature = lineDrawFeatures.get(sortedLineKeySet.get(i));
            if (feature != null) consumer.accept(feature);
        }
    }

    protected synchronized void forEachMultiColorLineDrawFeature(Consumer<MultiColorLineDrawFeature> consumer) {
        for (int i = 0; i < sortedMultiColorLineKeySet.size(); i++) {
            var feature = multiColorLineDrawFeatures.get(sortedMultiColorLineKeySet.get(i));
            if (feature != null) consumer.accept(feature);
        }
    }
}
