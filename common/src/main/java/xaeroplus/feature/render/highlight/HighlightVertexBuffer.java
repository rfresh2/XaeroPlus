package xaeroplus.feature.render.highlight;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DynamicUniforms;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import xaeroplus.feature.render.DrawContext;
import xaeroplus.feature.render.shaders.XaeroPlusShaders;
import xaeroplus.util.ChunkUtils;
import xaeroplus.util.ColorHelper;

import java.util.Optional;

public class HighlightVertexBuffer extends AbstractHighlightVertexBuffer {
    @Override
    public void preRender(final DrawContext ctx, final Long2LongMap highlights, final int color) {
        super.preRender(ctx, highlights, color);
    }

    @Override
    public void refresh(final DrawContext ctx, final Long2LongMap highlights, final int color) {
        stale = false;
        lastRefreshed = System.currentTimeMillis();
        flipped = ctx.worldmap();
        if (highlights.isEmpty() || ColorHelper.getA(color) == 0.0f) {
            close();
            return;
        }
        try (var byteBuffer = new ByteBufferBuilder(128)) {
            var bufferBuilder = new BufferBuilder(byteBuffer, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION);
            for (var it = highlights.keySet().iterator(); it.hasNext(); ) {
                var highlight = it.nextLong();
                var chunkPosX = ChunkUtils.longToChunkX(highlight);
                var chunkPosZ = ChunkUtils.longToChunkZ(highlight);
                float x1 = chunkPosX;
                float x2 = chunkPosX + 1;
                float y1 = flipped ? chunkPosZ + 1 : chunkPosZ;
                float y2 = flipped ? chunkPosZ : chunkPosZ + 1;
                bufferBuilder.addVertex(x1, y2, 0.0F);
                bufferBuilder.addVertex(x2, y2, 0.0F);
                bufferBuilder.addVertex(x2, y1, 0.0F);
                bufferBuilder.addVertex(x1, y1, 0.0F);
            }
            try (var meshData = bufferBuilder.buildOrThrow()) {
                close();
                vertexBuffer = RenderSystem.getDevice().createBuffer(() -> "Chunk Highlight Buffer", GpuBuffer.USAGE_VERTEX, meshData.vertexBuffer());
                indexCount = meshData.drawState().indexCount();
            }
        }
    }

    @Override
    public void render(DrawContext ctx, Long2LongMap highlights, int color) {
        if (vertexBuffer == null || vertexBuffer.isClosed() || uniformBuffer == null) return;
        var a = ColorHelper.getA(color);
        var r = ColorHelper.getR(color);
        var g = ColorHelper.getG(color);
        var b = ColorHelper.getB(color);
        uniformBuffer.rotate();
        try (var mappedView = uniformBuffer.currentBuffer().map(false, true)) {
            Std140Builder.intoBuffer(mappedView.data())
                .putMat4f(ctx.untranslatedMapViewMatrix())
                .putVec4(new Vector4f(r, g, b, a))
                .putVec2((float) Math.floorDiv(ctx.cameraBlockX(), 16), (float) Math.floorDiv(ctx.cameraBlockZ(), 16))
                .putVec2((float) Math.floorMod(ctx.cameraBlockX(), 16), (float) Math.floorMod(ctx.cameraBlockZ(), 16));
        }
        dynamicTransformUniformBuffer.rotate();
        try (var mappedView = dynamicTransformUniformBuffer.currentBuffer().map(false, true)) {
            var transform = new DynamicUniforms.Transform(RenderSystem.getModelViewStack(), new Vector4f(), new Vector3f(), new Matrix4f());
            transform.write(mappedView.data());
        }
        var autoIndexBuffer = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
        var indexType = autoIndexBuffer.type();
        var indexBuffer = autoIndexBuffer.getBuffer(indexCount);
        try (final RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
            .createRenderPass(() -> "XaeroPlus Highlight Vertex Buffer", Minecraft.getInstance().gameRenderer.mainRenderTarget().getColorTextureView(), Optional.empty())) {
            pass.setPipeline(XaeroPlusShaders.HIGHLIGHT_PIPELINE);
            RenderSystem.bindDefaultUniforms(pass); // Projection
            pass.setUniform("DynamicTransforms", dynamicTransformUniformBuffer.currentBuffer());
            pass.setUniform("HighlightTransforms", uniformBuffer.currentBuffer());
            pass.setIndexBuffer(indexBuffer, indexType);
            pass.setVertexBuffer(0, vertexBuffer.slice());
            pass.drawIndexed(indexCount, 1, 0, 0, 0);
        }
    }
}
