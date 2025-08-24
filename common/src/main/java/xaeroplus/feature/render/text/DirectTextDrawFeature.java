package xaeroplus.feature.render.text;

import xaeroplus.Globals;
import xaeroplus.util.ChunkUtils;

import java.util.Collection;

import static xaeroplus.util.GuiMapHelper.*;

public class DirectTextDrawFeature extends AbstractTextDrawFeature {
    private final TextSupplier textSupplier;
    private final String id;

    public DirectTextDrawFeature(String id, TextSupplier textSupplier) {
        this.id = id;
        this.textSupplier = textSupplier;
    }

    @Override
    public Collection<Text> getTexts() {
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
        return textSupplier.getText(windowX, windowZ, windowSize, Globals.getCurrentDimensionId()).values();
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public void invalidateCache() {}

    @Override
    public void close() {}
}

