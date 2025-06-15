package xaeroplus.feature.render;

import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectRBTreeMap;

import java.util.Comparator;
import java.util.function.Consumer;

public class DrawFeatureRegistry {
    private final Object2ObjectRBTreeMap<String, DrawFeature> features = new Object2ObjectRBTreeMap<>(Comparator.naturalOrder());

    public synchronized void register(DrawFeature feature) {
        var id = feature.id();
        unregister(id);
        features.put(id, feature);
    }

    public synchronized void unregister(String id) {
        features.remove(id);
    }

    public synchronized void forEach(Consumer<DrawFeature> action) {
        for (var entry : Object2ObjectMaps.fastIterable(features)) {
            var feature = entry.getValue();
            action.accept(feature);
        }
    }
}
