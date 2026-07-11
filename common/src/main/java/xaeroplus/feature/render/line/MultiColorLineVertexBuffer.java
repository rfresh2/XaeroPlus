package xaeroplus.feature.render.line;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
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
        setBufferOrigin(ctx);
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
            DrawHelper.addColoredLineQuadToExistingBuffer(
                bufferBuilder,
                x1 - bufferOriginBlockX,
                z1 - bufferOriginBlockZ,
                x2 - bufferOriginBlockX,
                z2 - bufferOriginBlockZ,
                r, g, b, alpha
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
            vertexBuffer = RenderSystem.getDevice().createBuffer(() -> "Multi Color Line Buffer", com.mojang.blaze3d.buffers.GpuBuffer.USAGE_VERTEX, meshData.vertexBuffer());
            indexCount = meshData.drawState().indexCount();
        }
    }

    @Override
    public void render(final DrawContext ctx, final float lineWidthScale) {
        if (vertexBuffer == null || vertexBuffer.isClosed() || uniformBuffer == null) return;
        uniformBuffer.rotate();
        try (var mappedView = RenderSystem.getDevice().createCommandEncoder().mapBuffer(uniformBuffer.currentBuffer(), false, true)) {
            Std140Builder.intoBuffer(mappedView.data())
                .putMat4f(ctx.untranslatedMapViewMatrix())
                .putVec2(XaeroPlusShaders.LINES_FRAME_SIZE[0], XaeroPlusShaders.LINES_FRAME_SIZE[1])
                .putFloat(lineWidthScale)
                .putVec2((float) ((long) bufferOriginBlockX - ctx.cameraBlockX()), (float) ((long) bufferOriginBlockZ - ctx.cameraBlockZ()));
        }
        GpuBufferSlice dynamic = RenderSystem.getDynamicUniforms()
            .writeTransform(RenderSystem.getModelViewMatrix(), new Vector4f(1.0f, 1.0f, 1.0f, 1.0f), new Vector3f(), new Matrix4f());
        var autoIndexBuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        var indexType = autoIndexBuffer.type();
        var indexBuffer = autoIndexBuffer.getBuffer(indexCount);
        try (final RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
            .createRenderPass(() -> "XaeroPlus Lines", Minecraft.getInstance().getMainRenderTarget().getColorTextureView(), OptionalInt.empty())) {
            pass.setPipeline(XaeroPlusShaders.LINES_PIPELINE);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", dynamic);
            pass.setUniform("LinesTransforms", uniformBuffer.currentBuffer());
            pass.setIndexBuffer(indexBuffer, indexType);
            pass.setVertexBuffer(0, vertexBuffer);
            pass.drawIndexed(0, 0, indexCount, 1);
        }
    }
}
