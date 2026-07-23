package xaeroplus.feature.render.ellipse;

import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import xaeroplus.feature.render.DrawContext;
import xaeroplus.feature.render.DrawHelper;
import xaeroplus.feature.render.shaders.XaeroPlusShaders;
import xaeroplus.util.ColorHelper;

import java.util.List;
import java.util.OptionalInt;

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
        var bufferBuilder = Tesselator.getInstance()
            .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
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
            vertexBuffer = RenderSystem.getDevice()
                .createBuffer(() -> "Ellipse Buffer", BufferType.VERTICES, BufferUsage.STATIC_WRITE, meshData.vertexBuffer());
            indexCount = meshData.drawState().indexCount();
        }
    }

    @Override
    public void render(final DrawContext ctx, final float thicknessScale) {
        if (vertexBuffer == null || vertexBuffer.isClosed()) return;
        var autoIndexBuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        var indexType = autoIndexBuffer.type();
        var indexBuffer = autoIndexBuffer.getBuffer(indexCount);
        try (final RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
            .createRenderPass(Minecraft.getInstance().getMainRenderTarget().getColorTexture(), OptionalInt.empty())) {
            pass.setPipeline(XaeroPlusShaders.ELLIPSES_PIPELINE);
            pass.setUniform("MapViewMatrix", ctx.untranslatedMapViewMatrix());
            pass.setUniform("ModelViewMat", RenderSystem.getModelViewMatrix());
            pass.setUniform("ProjMat", RenderSystem.getProjectionMatrix());
            pass.setUniform("FrameSize", XaeroPlusShaders.ELLIPSES_FRAME_SIZE);
            pass.setUniform("ColorModulator", RenderSystem.getShaderColor());
            pass.setUniform("CameraRelativeOrigin", (float) ((long) bufferOriginBlockX - ctx.cameraBlockX()), (float) ((long) bufferOriginBlockZ - ctx.cameraBlockZ()));
            pass.setUniform("Thickness", thicknessScale);
            pass.setIndexBuffer(indexBuffer, indexType);
            pass.setVertexBuffer(0, vertexBuffer);
            pass.drawIndexed(0, indexCount);
        }
    }
}
