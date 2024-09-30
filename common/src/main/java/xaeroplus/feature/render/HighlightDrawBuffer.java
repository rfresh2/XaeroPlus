package xaeroplus.feature.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.longs.LongList;
import org.jetbrains.annotations.Nullable;
import xaeroplus.util.ChunkUtils;

public class HighlightDrawBuffer {
    private boolean stale = true;
    @Nullable private VertexBuffer vertexBuffer = null;
    private boolean flipped = false;

    public boolean needsRefresh(boolean needsFlip) {
        return vertexBuffer == null || vertexBuffer.isInvalid() || stale || flipped != needsFlip;
    }

    public void refresh(LongList highlights, boolean needsFlip) {
        stale = false;
        flipped = needsFlip;
        if (highlights.isEmpty()) {
            close();
            return;
        }

        var bufferBuilder = Tesselator.getInstance()
            .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);

        for (int j = 0; j < highlights.size(); j++) {
            long highlight = highlights.getLong(j);
            var chunkPosX = ChunkUtils.longToChunkX(highlight);
            var chunkPosZ = ChunkUtils.longToChunkZ(highlight);
            float x1 = chunkPosX;
            float x2 = chunkPosX + 1;
            float y1 = needsFlip ? chunkPosZ + 1 : chunkPosZ;
            float y2 = needsFlip ? chunkPosZ : chunkPosZ + 1;
            bufferBuilder.addVertex(x1, y2, 0.0F);
            bufferBuilder.addVertex(x2, y2, 0.0F);
            bufferBuilder.addVertex(x2, y1, 0.0F);
            bufferBuilder.addVertex(x1, y1, 0.0F);
        }
        if (vertexBuffer == null || vertexBuffer.isInvalid()) {
            close();
            vertexBuffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
        }
        var meshData = bufferBuilder.buildOrThrow();
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
