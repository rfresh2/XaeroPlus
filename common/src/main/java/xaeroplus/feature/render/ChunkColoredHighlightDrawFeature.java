package xaeroplus.feature.render;

import it.unimi.dsi.fastutil.longs.Long2LongMap;

public interface ChunkColoredHighlightDrawFeature {
    Long2LongMap getChunkHighlights();
    int getColorAlpha();
    void render(boolean worldmap);
    void invalidateCache();
    void close();
}
