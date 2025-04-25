package xaeroplus.feature.render;

import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import org.jetbrains.annotations.Nullable;
import xaeroplus.util.ChunkUtils;

public class HighlightDrawBuffer {
    private boolean stale = true;
    @Nullable private GpuBuffer vertexBuffer = null;
    private boolean flipped = false;
    long lastRefreshed = 0L;
    int indexCount = 0;

    public boolean needsRefresh(boolean needsFlip) {
        return vertexBuffer == null || vertexBuffer.isClosed() || stale || flipped != needsFlip;
    }

    public void refresh(Long2LongMap highlights, boolean needsFlip) {
        stale = false;
        lastRefreshed = System.currentTimeMillis();
        flipped = needsFlip;
        if (highlights.isEmpty()) {
            close();
            return;
        }
        var bufferBuilder = Tesselator.getInstance()
            .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        for (var highlight : highlights.keySet()) {
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
        try (var meshData = bufferBuilder.buildOrThrow()) {
            close();
            vertexBuffer = RenderSystem.getDevice().createBuffer(() -> "Chunk Highlight Buffer", BufferType.VERTICES, BufferUsage.STATIC_WRITE, meshData.vertexBuffer());
            indexCount = meshData.drawState().indexCount();
        }
    }

    public int indexCount() {
        return indexCount;
    }

    public void render(final RenderPass pass, final GpuBuffer indexBuffer, final VertexFormat.IndexType indexType) {
        if (vertexBuffer == null || vertexBuffer.isClosed()) return;
        pass.setIndexBuffer(indexBuffer, indexType);
        pass.setVertexBuffer(0, vertexBuffer);
        pass.drawIndexed(0, indexCount);
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
