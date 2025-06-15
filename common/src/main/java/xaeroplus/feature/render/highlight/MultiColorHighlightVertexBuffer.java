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

public class MultiColorHighlightVertexBuffer extends AbstractHighlightVertexBuffer {

    @Override
    public void preRender(final DrawContext ctx, final Long2LongMap highlights, final int color) {
        super.preRender(ctx, highlights, color);
        XaeroPlusShaders.setMultiColorMapViewMatrix(ctx.matrixStack().last().pose());
    }

    public void refresh(DrawContext ctx, Long2LongMap highlights, int colorAlpha) {
        stale = false;
        lastRefreshed = System.currentTimeMillis();
        flipped = ctx.worldmap();
        if (highlights.isEmpty() || colorAlpha == 0) {
            close();
            return;
        }
        var bufferBuilder = Tesselator.getInstance()
            .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (var entry : highlights.long2LongEntrySet()) {
            var highlight = entry.getLongKey();
            int color = ColorHelper.getColorWithAlpha((int) entry.getLongValue(), colorAlpha);
            var chunkPosX = ChunkUtils.longToChunkX(highlight);
            var chunkPosZ = ChunkUtils.longToChunkZ(highlight);
            float x1 = chunkPosX;
            float x2 = chunkPosX + 1;
            float y1 = flipped ? chunkPosZ + 1 : chunkPosZ;
            float y2 = flipped ? chunkPosZ : chunkPosZ + 1;
            bufferBuilder.addVertex(x1, y2, 0F).setColor(color);
            bufferBuilder.addVertex(x2, y2, 0F).setColor(color);
            bufferBuilder.addVertex(x2, y1, 0F).setColor(color);
            bufferBuilder.addVertex(x1, y1, 0F).setColor(color);
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
            .getProgram(XaeroPlusShaders.MULTI_COLOR_HIGHLIGHT_SHADER_PROGRAM);
    }
}
