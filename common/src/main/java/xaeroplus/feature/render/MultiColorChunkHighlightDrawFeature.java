package xaeroplus.feature.render;

import it.unimi.dsi.fastutil.longs.Long2LongMap;

public interface MultiColorChunkHighlightDrawFeature {
    Long2LongMap getChunkHighlights();
    int getColorAlpha();
    void render(boolean worldmap);
    void invalidateCache();
    void close();
}
