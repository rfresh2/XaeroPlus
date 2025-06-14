package xaeroplus.feature.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.longs.Long2LongMap;

public interface MultiColorChunkHighlightDrawFeature {
    Long2LongMap getChunkHighlights();
    int getColorAlpha();
    void render(boolean worldmap, final RenderPass pass, final GpuBuffer indexBuffer, final VertexFormat.IndexType indexType);
    int indexCount();
    void refreshIfNeeded(boolean worldmap);
    void invalidateCache();
    void close();
}
