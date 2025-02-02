package xaeroplus.feature.render;

import it.unimi.dsi.fastutil.longs.Long2LongMap;

public interface ChunkHighlightDrawFeature {
    Long2LongMap getChunkHighlights();
    int colorInt();
    void render(boolean worldmap);
    void invalidateCache();
    void close();
}
