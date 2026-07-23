package xaeroplus.feature.render.ellipse;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DynamicUniforms;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import xaeroplus.feature.render.DrawContext;
import xaeroplus.feature.render.DrawHelper;
import xaeroplus.feature.render.shaders.XaeroPlusShaders;
import xaeroplus.util.ColorHelper;

import java.util.Optional;

public class MultiColorEllipseVertexBuffer extends AbstractEllipseVertexBuffer<Object2IntMap<Ellipse>> {
    private final MultiColorEllipseColorFunction colorFunction;

    public MultiColorEllipseVertexBuffer(final MultiColorEllipseColorFunction colorFunction) {
        this.colorFunction = colorFunction;
    }

    @Override
    protected void refresh(final DrawContext ctx, final Object2IntMap<Ellipse> ellipses) {
        stale = false;
        flipped = ctx.worldmap();
        if (ellipses.isEmpty()) {
            close();
            return;
        }
        setBufferOrigin(ctx);
        try (var byteBuffer = new ByteBufferBuilder(128)) {
            var bufferBuilder = new BufferBuilder(byteBuffer, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            var hasVertices = false;
            var iterator = Object2IntMaps.fastIterator(ellipses);
            while (iterator.hasNext()) {
                var entry = iterator.next();
                var ellipse = entry.getKey();
                var color = colorFunction.getColor(ellipse, entry.getIntValue());
                var alpha = ColorHelper.getA(color);
                if (alpha == 0.0f) continue;
                DrawHelper.addColoredEllipseQuadToExistingBuffer(
                    bufferBuilder,
                    ellipse.centerX() - bufferOriginBlockX,
                    ellipse.centerZ() - bufferOriginBlockZ,
                    ellipse.radiusX(),
                    ellipse.radiusZ(),
                    ColorHelper.getR(color),
                    ColorHelper.getG(color),
                    ColorHelper.getB(color),
                    alpha
                );
                hasVertices = true;
            }
            if (!hasVertices) {
                close();
                return;
            }
            var meshData = bufferBuilder.build();
            if (meshData == null) {
                close();
                return;
            }
            try (meshData) {
                close();
                vertexBuffer = RenderSystem.getDevice().createBuffer(
                    () -> "Multi Color Ellipse Buffer",
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
