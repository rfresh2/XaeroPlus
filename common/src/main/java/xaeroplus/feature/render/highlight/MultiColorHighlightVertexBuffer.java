package xaeroplus.feature.render.highlight;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongMaps;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import xaeroplus.feature.render.DrawContext;
import xaeroplus.feature.render.shaders.XaeroPlusShaders;
import xaeroplus.util.ChunkUtils;
import xaeroplus.util.ColorHelper;

import java.util.OptionalInt;

public class MultiColorHighlightVertexBuffer extends AbstractHighlightVertexBuffer {
    private final MultiColorHighlightColorFunction colorFunction;

    public MultiColorHighlightVertexBuffer(final MultiColorHighlightColorFunction colorFunction) {
        this.colorFunction = colorFunction;
    }

    @Override
    public void preRender(final DrawContext ctx, final Long2LongMap highlights, final int color) {
        super.preRender(ctx, highlights, color);
    }

    public void refresh(DrawContext ctx, Long2LongMap highlights, int colorAlpha) {
        stale = false;
        lastRefreshed = System.currentTimeMillis();
        flipped = ctx.worldmap();
        if (highlights.isEmpty()) {
            close();
            return;
        }
        var bufferBuilder = Tesselator.getInstance()
            .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        var it = Long2LongMaps.fastIterator(highlights);
        while (it.hasNext()) {
            var entry = it.next();
            var pos = entry.getLongKey();
            long foundTime = entry.getLongValue();
            int color = colorFunction.getColor(pos, foundTime);
            int alpha = ColorHelper.getIntA(color);
            if (alpha == 0) continue;
            var chunkPosX = ChunkUtils.longToChunkX(pos);
            var chunkPosZ = ChunkUtils.longToChunkZ(pos);
            float x1 = chunkPosX;
            float x2 = chunkPosX + 1;
            float y1 = flipped ? chunkPosZ + 1 : chunkPosZ;
            float y2 = flipped ? chunkPosZ : chunkPosZ + 1;
            bufferBuilder.addVertex(x1, y2, 0F).setColor(color);
            bufferBuilder.addVertex(x2, y2, 0F).setColor(color);
            bufferBuilder.addVertex(x2, y1, 0F).setColor(color);
            bufferBuilder.addVertex(x1, y1, 0F).setColor(color);
        }
        var meshData = bufferBuilder.build();
        if (meshData == null) {
            close();
            return;
        }
        try (meshData) {
            close();
            vertexBuffer = RenderSystem.getDevice().createBuffer(() -> "Chunk Highlight Buffer", GpuBuffer.USAGE_VERTEX, meshData.vertexBuffer());
            indexCount = meshData.drawState().indexCount();
        }
    }

    @Override
    public void render(DrawContext ctx, Long2LongMap highlights, int color) {
        if (vertexBuffer == null || vertexBuffer.isClosed() || uniformBuffer == null) return;
        uniformBuffer.rotate();
        try (var mappedView = RenderSystem.getDevice().createCommandEncoder().mapBuffer(uniformBuffer.currentBuffer(), false, true)) {
            Std140Builder.intoBuffer(mappedView.data())
                .putMat4f(ctx.untranslatedMapViewMatrix())
                .putVec2((float) Math.floorDiv(ctx.cameraBlockX(), 16), (float) Math.floorDiv(ctx.cameraBlockZ(), 16))
                .putVec2((float) Math.floorMod(ctx.cameraBlockX(), 16), (float) Math.floorMod(ctx.cameraBlockZ(), 16));
        }
        GpuBufferSlice dynamic = RenderSystem.getDynamicUniforms()
            // only need ModelViewMat
            .writeTransform(RenderSystem.getModelViewMatrix(), new Vector4f(), new Vector3f(), new Matrix4f(), 0);
        var autoIndexBuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        var indexType = autoIndexBuffer.type();
        var indexBuffer = autoIndexBuffer.getBuffer(indexCount);
        try (final RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
            .createRenderPass(() -> "XaeroPlus Highlight Vertex Buffer", Minecraft.getInstance().getMainRenderTarget().getColorTextureView(), OptionalInt.empty())) {
            pass.setPipeline(XaeroPlusShaders.MULTI_COLOR_HIGHLIGHT_PIPELINE);
            RenderSystem.bindDefaultUniforms(pass); // Projection
            pass.setUniform("DynamicTransforms", dynamic);
            pass.setUniform("MultiColorHighlightTransforms", uniformBuffer.currentBuffer());
            pass.setIndexBuffer(indexBuffer, indexType);
            pass.setVertexBuffer(0, vertexBuffer);
            pass.drawIndexed(0, 0, indexCount, 1);
        }
    }
}
