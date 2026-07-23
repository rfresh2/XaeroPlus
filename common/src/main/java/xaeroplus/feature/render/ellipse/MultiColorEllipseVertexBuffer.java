package xaeroplus.feature.render.ellipse;

import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import net.minecraft.client.Minecraft;
import xaeroplus.feature.render.DrawContext;
import xaeroplus.feature.render.DrawHelper;
import xaeroplus.feature.render.shaders.XaeroPlusShaders;
import xaeroplus.util.ColorHelper;

import java.util.OptionalInt;

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
        var bufferBuilder = Tesselator.getInstance()
            .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
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
            vertexBuffer = RenderSystem.getDevice()
                .createBuffer(() -> "Multi Color Ellipse Buffer", BufferType.VERTICES, BufferUsage.STATIC_WRITE, meshData.vertexBuffer());
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
