package xaeroplus.feature.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.longs.LongList;
import org.jetbrains.annotations.Nullable;
import xaeroplus.util.ChunkUtils;

public class HighlightDrawBuffer {
    private boolean stale = true;
    @Nullable private VertexBuffer vertexBuffer = null;
    // todo: it'd be REALLY nice if we could share these buffers across the worldmap AND minimap
    //  but i need to figure out some solution to the coordinates being flipped on the worldmap (to match the mc world's coordinate system)
    //  if i apply a flip to the model view matrix it fails to draw as the quad vertex ordering needs to be in CCW order
    //  so unless that can fixed the only solution i see is to draw to a new fbo, flip the fbo, and then draw that on top
    private final boolean flipped;

    public HighlightDrawBuffer(boolean flipped) {
        this.flipped = flipped;
    }

    public boolean needsRefresh() {
        return vertexBuffer == null || vertexBuffer.isInvalid() || stale;
    }

    public void refresh(LongList highlights) {
        stale = false;
        if (highlights.isEmpty()) {
            close();
            return;
        }
        var bufferBuilder = new BufferBuilder(128);
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);

        for (int j = 0; j < highlights.size(); j++) {
            long highlight = highlights.getLong(j);
            var chunkPosX = ChunkUtils.longToChunkX(highlight);
            var chunkPosZ = ChunkUtils.longToChunkZ(highlight);
            float x1 = chunkPosX;
            float x2 = chunkPosX + 1;
            float y1 = chunkPosZ;
            float y2 = chunkPosZ + 1;
            if (flipped) {
                float t = y1;
                y1 = y2;
                y2 = t;
            }
            bufferBuilder.vertex(x1, y2, 0F).endVertex();
            bufferBuilder.vertex(x2, y2, 0F).endVertex();
            bufferBuilder.vertex(x2, y1, 0F).endVertex();
            bufferBuilder.vertex(x1, y1, 0F).endVertex();
        }
        if (vertexBuffer == null || vertexBuffer.isInvalid()) {
            close();
            vertexBuffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
        }
        var meshData = bufferBuilder.end();
        vertexBuffer.bind();
        vertexBuffer.upload(meshData);
    }

    public void render() {
        if (vertexBuffer == null || vertexBuffer.isInvalid()) return;
        var shader = XaeroPlusShaders.HIGHLIGHT_SHADER;
        if (shader == null) return;
        vertexBuffer.bind();
        vertexBuffer.drawWithShader(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), shader);
    }

    public void markStale() {
        stale = true;
    }

    public void close() {
        if (vertexBuffer != null) {
            vertexBuffer.close();
            vertexBuffer = null;
        }
    }
}
