package xaeroplus.feature.render.highlight;

import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CompiledShaderProgram;
import xaeroplus.feature.render.DrawContext;
import xaeroplus.feature.render.shaders.XaeroPlusShaders;
import xaeroplus.util.ChunkUtils;
import xaeroplus.util.ColorHelper;

public class HighlightVertexBuffer extends AbstractHighlightVertexBuffer {

    @Override
    public void preRender(final DrawContext ctx, final Long2LongMap highlights, final int color) {
        super.preRender(ctx, highlights, color);
        XaeroPlusShaders.setMapViewMatrix(ctx.matrixStack().last().pose());
        var a = ColorHelper.getA(color);
        var r = ColorHelper.getR(color);
        var g = ColorHelper.getG(color);
        var b = ColorHelper.getB(color);
        XaeroPlusShaders.setHighlightColor(r, g, b, a);
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
        var bufferBuilder = Tesselator.getInstance()
            .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        for (var highlight : highlights.keySet()) {
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
        if (vertexBuffer == null || vertexBuffer.isInvalid()) {
            close();
            vertexBuffer = new VertexBuffer(BufferUsage.STATIC_WRITE);
        }
        var meshData = bufferBuilder.buildOrThrow();
        vertexBuffer.bind();
        vertexBuffer.upload(meshData);
    }

    @Override
    public CompiledShaderProgram shaderInstance() {
        return Minecraft.getInstance()
            .getShaderManager()
            .getProgram(XaeroPlusShaders.HIGHLIGHT_SHADER_PROGRAM);
    }
}
