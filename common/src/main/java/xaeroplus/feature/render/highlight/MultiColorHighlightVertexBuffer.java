package xaeroplus.feature.render.highlight;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import net.minecraft.client.renderer.ShaderInstance;
import xaeroplus.feature.render.DrawContext;
import xaeroplus.feature.render.shaders.XaeroPlusShaders;
import xaeroplus.util.ChunkUtils;
import xaeroplus.util.ColorHelper;

public class MultiColorHighlightVertexBuffer extends AbstractHighlightVertexBuffer {
    private final MultiColorHighlightColorFunction colorFunction;

    public MultiColorHighlightVertexBuffer(final MultiColorHighlightColorFunction colorFunction) {
        this.colorFunction = colorFunction;
    }

    @Override
    public void preRender(final DrawContext ctx, final Long2LongMap highlights, final int color) {
        super.preRender(ctx, highlights, color);
        var shader = XaeroPlusShaders.MULTI_COLOR_HIGHLIGHT_SHADER;
        shader.setMapViewMatrix(ctx.matrixStack().last().pose());
    }

    public void refresh(DrawContext ctx, Long2LongMap highlights, int colorAlpha) {
        stale = false;
        lastRefreshed = System.currentTimeMillis();
        flipped = ctx.worldmap();
        if (highlights.isEmpty()) {
            close();
            return;
        }
        var bufferBuilder = Tesselator.getInstance()
            .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        boolean hasEntries = false;
        for (var entry : highlights.long2LongEntrySet()) {
            var pos = entry.getLongKey();
            long foundTime = entry.getLongValue();
            int color = colorFunction.getColor(pos, foundTime);
            int alpha = ColorHelper.getIntA(color);
            if (alpha == 0) continue;
            var chunkPosX = ChunkUtils.longToChunkX(pos);
            var chunkPosZ = ChunkUtils.longToChunkZ(pos);
            float x1 = chunkPosX;
            float x2 = chunkPosX + 1;
            float y1 = flipped ? chunkPosZ + 1 : chunkPosZ;
            float y2 = flipped ? chunkPosZ : chunkPosZ + 1;
            bufferBuilder.addVertex(x1, y2, 0F).setColor(color);
            bufferBuilder.addVertex(x2, y2, 0F).setColor(color);
            bufferBuilder.addVertex(x2, y1, 0F).setColor(color);
            bufferBuilder.addVertex(x1, y1, 0F).setColor(color);
            hasEntries = true;
        }
        if (!hasEntries) {
            close();
            return;
        }
        if (vertexBuffer == null || vertexBuffer.isInvalid()) {
            close();
            vertexBuffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
        }
        var meshData = bufferBuilder.buildOrThrow();
        vertexBuffer.bind();
        vertexBuffer.upload(meshData);
    }

    @Override
    public ShaderInstance shaderInstance() {
        return XaeroPlusShaders.MULTI_COLOR_HIGHLIGHT_SHADER;
    }
}
