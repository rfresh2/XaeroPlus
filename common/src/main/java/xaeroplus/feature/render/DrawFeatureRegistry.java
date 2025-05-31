package xaeroplus.feature.render;

import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;
import xaeroplus.settings.Settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class DrawFeatureRegistry {
    private final Int2ObjectRBTreeMap<DrawFeature> features = new Int2ObjectRBTreeMap<>(Comparator.naturalOrder());
    private final List<String> drawOrder = new ArrayList<>();

    public synchronized void register(DrawFeature feature) {
        var id = feature.id();
        unregister(id);
        for (int i = 0; i < drawOrder.size(); i++) {
            var entryId = drawOrder.get(i);
            if (entryId.equals(id)) {
                features.put(i, feature);
                return;
            }
        }
        drawOrder.add(id);
        features.put(drawOrder.indexOf(id), feature);
        var serialized = String.join(",", drawOrder);
        Settings.REGISTRY.drawOrderSetting.setValue(serialized);
    }

    public synchronized void unregister(String id) {
        var it = Int2ObjectMaps.fastIterator(features);
        while (it.hasNext()) {
            var entry = it.next();
            if (entry.getValue().id().equals(id)) {
                it.remove();
            }
        }
    }

    public synchronized void forEach(Consumer<DrawFeature> action) {
        for (var entry : Int2ObjectMaps.fastIterable(features)) {
            var feature = entry.getValue();
            action.accept(feature);
        }
    }

    public synchronized void loadOrder(final String setting) {
        String[] featureIds = setting.split(",");
        drawOrder.clear();
        drawOrder.addAll(Arrays.asList(featureIds));
    }
}
