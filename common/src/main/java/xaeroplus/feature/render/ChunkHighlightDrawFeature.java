package xaeroplus.feature.render;

import com.mojang.blaze3d.systems.RenderPass;
import it.unimi.dsi.fastutil.longs.Long2LongMap;

public interface ChunkHighlightDrawFeature {
    Long2LongMap getChunkHighlights();
    int colorInt();
    void render(boolean worldmap, final RenderPass pass);
    void refreshIfNeeded(boolean worldmap);
    void invalidateCache();
    void close();
}
