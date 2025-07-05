package xaeroplus.feature.render;

import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;
import net.minecraft.client.Minecraft;
import xaeroplus.Globals;
import xaeroplus.XaeroPlus;
import xaeroplus.settings.Settings;
import xaeroplus.util.DrawOrderHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DrawFeatureRegistry {
    private final Int2ObjectRBTreeMap<DrawFeature> features = new Int2ObjectRBTreeMap<>(Comparator.naturalOrder());
    private final List<String> drawOrder = new ArrayList<>();

    private boolean validFeatureId(String id) {
        return Pattern.compile("[a-zA-Z0-9_-]+").matcher(id).find();
    }

    public synchronized void register(DrawFeature feature) {
        assertOnMainThread();
        var id = feature.id();
        if (!validFeatureId(id)) {
            throw new IllegalStateException("Invalid feature id " + id + "\nMust be only letters, numbers, or '_' or '-' characters");
        }
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
        String serialized = DrawOrderHelper.serialize(drawOrder);
        Settings.REGISTRY.drawOrderSetting.setValue(serialized);
    }

    public synchronized void unregister(String id) {
        assertOnMainThread();
        var it = Int2ObjectMaps.fastIterator(features);
        while (it.hasNext()) {
            var entry = it.next();
            if (entry.getValue().id().equals(id)) {
                it.remove();
            }
        }
    }

    public synchronized void forEach(Consumer<DrawFeature> action) {
        assertOnMainThread();
        for (var entry : Int2ObjectMaps.fastIterable(features)) {
            var feature = entry.getValue();
            action.accept(feature);
        }
    }

    public synchronized void loadOrder(final String setting) {
        assertOnMainThread();
        List<String> featureIds = DrawOrderHelper.load(setting);
        drawOrder.clear();
        drawOrder.addAll(featureIds);
        var featuresCopy = new ArrayList<DrawFeature>(featureIds.size());
        featuresCopy.addAll(features.values());
        features.clear();
        for (int i = 0; i < drawOrder.size(); i++) {
            var entryId = drawOrder.get(i);
            for (var feature : featuresCopy) {
                if (entryId.equals(feature.id())) {
                    features.put(i, feature);
                    featuresCopy.remove(feature);
                    break;
                }
            }
        }
        if (!featuresCopy.isEmpty()) {
            XaeroPlus.LOGGER.error("Unknown draw features removed after reordering: {}", featuresCopy.stream().map(DrawFeature::id).collect(Collectors.joining(", ")));
        }
    }

    private void assertOnMainThread() {
        if (Globals.minimapSettingsInitialized) {
            if (!Minecraft.getInstance().isSameThread()) {
                throw new RuntimeException("Not on main thread!");
            }
        }
    }
}
