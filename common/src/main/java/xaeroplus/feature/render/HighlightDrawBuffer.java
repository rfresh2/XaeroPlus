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
            var blockPosX = ChunkUtils.chunkCoordToCoord(chunkPosX);
            var blockPosZ = ChunkUtils.chunkCoordToCoord(chunkPosZ);
            var chunkSize = 16;
            float x1 = blockPosX;
            float x2 = blockPosX + chunkSize;
            float y1 = blockPosZ;
            float y2 = blockPosZ + chunkSize;
            bufferBuilder.vertex(x1, y1, 0.0F).endVertex();
            bufferBuilder.vertex(x2, y1, 0.0F).endVertex();
            bufferBuilder.vertex(x2, y2, 0.0F).endVertex();
            bufferBuilder.vertex(x1, y2, 0.0F).endVertex();
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
