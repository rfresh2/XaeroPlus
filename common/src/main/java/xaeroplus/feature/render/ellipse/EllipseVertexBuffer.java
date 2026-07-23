package xaeroplus.feature.render.ellipse;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DynamicUniforms;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import xaeroplus.feature.render.DrawContext;
import xaeroplus.feature.render.DrawHelper;
import xaeroplus.feature.render.shaders.XaeroPlusShaders;
import xaeroplus.util.ColorHelper;

import java.util.List;
import java.util.Optional;

public class EllipseVertexBuffer extends AbstractEllipseVertexBuffer<List<Ellipse>> {
    private int color = -1;

    public void setColor(final int color) {
        if (this.color != color) {
            this.color = color;
            markStale();
        }
    }

    @Override
    protected void refresh(final DrawContext ctx, final List<Ellipse> ellipses) {
        stale = false;
        flipped = ctx.worldmap();
        if (ellipses.isEmpty() || ColorHelper.getA(color) == 0.0f) {
            close();
            return;
        }
        setBufferOrigin(ctx);
        var r = ColorHelper.getR(color);
        var g = ColorHelper.getG(color);
        var b = ColorHelper.getB(color);
        var a = ColorHelper.getA(color);
        try (var byteBuffer = new ByteBufferBuilder(128)) {
            var bufferBuilder = new BufferBuilder(byteBuffer, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            for (var ellipse : ellipses) {
                DrawHelper.addColoredEllipseQuadToExistingBuffer(
                    bufferBuilder,
                    ellipse.centerX() - bufferOriginBlockX,
                    ellipse.centerZ() - bufferOriginBlockZ,
                    ellipse.radiusX(),
                    ellipse.radiusZ(),
                    r, g, b, a
                );
            }
            try (var meshData = bufferBuilder.buildOrThrow()) {
                close();
                vertexBuffer = RenderSystem.getDevice().createBuffer(
                    () -> "Ellipse Buffer",
                    GpuBuffer.USAGE_VERTEX,
                    meshData.vertexBuffer()
                );
                indexCount = meshData.drawState().indexCount();
            }
        }
    }

    @Override
    public void render(final DrawContext ctx, final float thicknessScale) {
        if (vertexBuffer == null || vertexBuffer.isClosed() || uniformBuffer == null) return;
        uniformBuffer.rotate();
        try (var mappedView = uniformBuffer.currentBuffer().map(false, true)) {
            Std140Builder.intoBuffer(mappedView.data())
                .putMat4f(ctx.untranslatedMapViewMatrix())
                .putVec2(XaeroPlusShaders.ELLIPSES_FRAME_SIZE[0], XaeroPlusShaders.ELLIPSES_FRAME_SIZE[1])
                .putFloat(thicknessScale)
                .putVec2((float) ((long) bufferOriginBlockX - ctx.cameraBlockX()), (float) ((long) bufferOriginBlockZ - ctx.cameraBlockZ()));
        }
        dynamicTransformUniformBuffer.rotate();
        try (var mappedView = dynamicTransformUniformBuffer.currentBuffer().map(false, true)) {
            var transform = new DynamicUniforms.Transform(RenderSystem.getModelViewStack(), new Vector4f(1.0f, 1.0f, 1.0f, 1.0f), new Vector3f(), new Matrix4f());
            transform.write(mappedView.data());
        }
        var autoIndexBuffer = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
        var indexType = autoIndexBuffer.type();
        var indexBuffer = autoIndexBuffer.getBuffer(indexCount);
        try (final RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
            .createRenderPass(() -> "XaeroPlus Ellipses", Minecraft.getInstance().gameRenderer.mainRenderTarget().getColorTextureView(), Optional.empty())) {
            pass.setPipeline(XaeroPlusShaders.ELLIPSES_PIPELINE);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", dynamicTransformUniformBuffer.currentBuffer());
            pass.setUniform("EllipsesTransforms", uniformBuffer.currentBuffer());
            pass.setIndexBuffer(indexBuffer, indexType);
            pass.setVertexBuffer(0, vertexBuffer.slice());
            pass.drawIndexed(indexCount, 1, 0, 0, 0);
        }
    }
}
