package xaeroplus.feature.render.line;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import xaeroplus.feature.render.DrawContext;
import xaeroplus.feature.render.DrawHelper;
import xaeroplus.feature.render.shaders.XaeroPlusShaders;
import xaeroplus.util.ColorHelper;

import java.util.Optional;

public class LineVertexBuffer extends AbstractLineVertexBuffer<java.util.List<Line>> {
    private int color = -1;

    public void setColor(final int color) {
        if (this.color != color) {
            this.color = color;
            markStale();
        }
    }

    @Override
    protected void refresh(final DrawContext ctx, final java.util.List<Line> lines) {
        stale = false;
        flipped = ctx.worldmap();
        if (lines.isEmpty() || ColorHelper.getA(color) == 0.0f) {
            close();
            return;
        }
        var r = ColorHelper.getR(color);
        var g = ColorHelper.getG(color);
        var b = ColorHelper.getB(color);
        var a = ColorHelper.getA(color);
        try (var byteBuffer = new ByteBufferBuilder(128)) {
            var bufferBuilder = new BufferBuilder(byteBuffer, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            for (int i = 0; i < lines.size(); i++) {
                var line = lines.get(i);
                int x1 = flipped ? line.x2() : line.x1();
                int z1 = flipped ? line.z2() : line.z1();
                int x2 = flipped ? line.x1() : line.x2();
                int z2 = flipped ? line.z1() : line.z2();
                DrawHelper.addColoredLineQuadToExistingBuffer(bufferBuilder, x1, z1, x2, z2, r, g, b, a);
            }
            try (var meshData = bufferBuilder.buildOrThrow()) {
                close();
                vertexBuffer = RenderSystem.getDevice().createBuffer(() -> "Line Buffer", GpuBuffer.USAGE_VERTEX, meshData.vertexBuffer());
                indexCount = meshData.drawState().indexCount();
            }
        }
    }

    @Override
    public void render(final DrawContext ctx, final float lineWidthScale) {
        if (vertexBuffer == null || vertexBuffer.isClosed() || uniformBuffer == null) return;
        uniformBuffer.rotate();
        try (var mappedView = uniformBuffer.currentBuffer().map(false, true)) {
            Std140Builder.intoBuffer(mappedView.data())
                .putMat4f(ctx.matrixStack().last().pose())
                .putVec2(XaeroPlusShaders.LINES_FRAME_SIZE[0], XaeroPlusShaders.LINES_FRAME_SIZE[1])
                .putFloat(lineWidthScale);
        }
        GpuBufferSlice dynamic = RenderSystem.getDynamicUniforms()
            .writeTransform(RenderSystem.getModelViewStack(), new Vector4f(1.0f, 1.0f, 1.0f, 1.0f), new Vector3f(), new Matrix4f());
        var autoIndexBuffer = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
        var indexType = autoIndexBuffer.type();
        var indexBuffer = autoIndexBuffer.getBuffer(indexCount);
        try (final RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
            .createRenderPass(() -> "XaeroPlus Lines",  Minecraft.getInstance().gameRenderer.mainRenderTarget().getColorTextureView(), Optional.empty())) {
            pass.setPipeline(XaeroPlusShaders.LINES_PIPELINE);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", dynamic);
            pass.setUniform("LinesTransforms", uniformBuffer.currentBuffer());
            pass.setIndexBuffer(indexBuffer, indexType);
            pass.setVertexBuffer(0, vertexBuffer.slice());
            pass.drawIndexed(indexCount, 1, 0, 0, 0);
        }
    }
}
