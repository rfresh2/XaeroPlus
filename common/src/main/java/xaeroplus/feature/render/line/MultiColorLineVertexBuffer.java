package xaeroplus.feature.render.line;

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

public class MultiColorLineVertexBuffer extends AbstractLineVertexBuffer<Object2IntMap<Line>> {
    private final MultiColorLineColorFunction colorFunction;

    public MultiColorLineVertexBuffer(final MultiColorLineColorFunction colorFunction) {
        this.colorFunction = colorFunction;
    }

    @Override
    protected void refresh(final DrawContext ctx, final Object2IntMap<Line> lines) {
        stale = false;
        flipped = ctx.worldmap();
        if (lines.isEmpty()) {
            close();
            return;
        }
        var bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        boolean hasVertices = false;
        var it = Object2IntMaps.fastIterator(lines);
        while (it.hasNext()) {
            var entry = it.next();
            var line = entry.getKey();
            var color = colorFunction.getColor(line, entry.getIntValue());
            var alpha = ColorHelper.getA(color);
            if (alpha == 0.0f) continue;
            var r = ColorHelper.getR(color);
            var g = ColorHelper.getG(color);
            var b = ColorHelper.getB(color);
            int x1 = flipped ? line.x2() : line.x1();
            int z1 = flipped ? line.z2() : line.z1();
            int x2 = flipped ? line.x1() : line.x2();
            int z2 = flipped ? line.z1() : line.z2();
            DrawHelper.addColoredLineQuadToExistingBuffer(bufferBuilder, x1, z1, x2, z2, r, g, b, alpha);
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
                .createBuffer(() -> "Multi Color Line Buffer", BufferType.VERTICES, BufferUsage.STATIC_WRITE, meshData.vertexBuffer());
            indexCount = meshData.drawState().indexCount();
        }
    }

    @Override
    public void render(final DrawContext ctx, final float lineWidthScale) {
        if (vertexBuffer == null || vertexBuffer.isClosed()) return;
        var autoIndexBuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        var indexType = autoIndexBuffer.type();
        var indexBuffer = autoIndexBuffer.getBuffer(indexCount);
        try (final RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
            .createRenderPass(Minecraft.getInstance().getMainRenderTarget().getColorTexture(), OptionalInt.empty())) {
            pass.setPipeline(XaeroPlusShaders.LINES_PIPELINE);
            pass.setUniform("MapViewMatrix", ctx.matrixStack().last().pose());
            pass.setUniform("ModelViewMat", RenderSystem.getModelViewMatrix());
            pass.setUniform("ProjMat", RenderSystem.getProjectionMatrix());
            pass.setUniform("FrameSize", XaeroPlusShaders.LINES_FRAME_SIZE);
            pass.setUniform("ColorModulator", RenderSystem.getShaderColor());
            pass.setUniform("LineWidth", lineWidthScale);
            pass.setIndexBuffer(indexBuffer, indexType);
            pass.setVertexBuffer(0, vertexBuffer);
            pass.drawIndexed(0, indexCount);
        }
    }
}
