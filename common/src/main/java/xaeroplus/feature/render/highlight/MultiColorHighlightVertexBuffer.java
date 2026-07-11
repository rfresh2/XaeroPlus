package xaeroplus.feature.render.highlight;

import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongMaps;
import net.minecraft.client.Minecraft;
import xaeroplus.feature.render.DrawContext;
import xaeroplus.feature.render.shaders.XaeroPlusShaders;
import xaeroplus.util.ChunkUtils;
import xaeroplus.util.ColorHelper;

import java.util.OptionalInt;

public class MultiColorHighlightVertexBuffer extends AbstractHighlightVertexBuffer {
    private final MultiColorHighlightColorFunction colorFunction;

    public MultiColorHighlightVertexBuffer(final MultiColorHighlightColorFunction colorFunction) {
        this.colorFunction = colorFunction;
    }

    @Override
    public void preRender(final DrawContext ctx, final Long2LongMap highlights, final int color) {
        super.preRender(ctx, highlights, color);
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
        var it = Long2LongMaps.fastIterator(highlights);
        while (it.hasNext()) {
            var entry = it.next();
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
        }
        var meshData = bufferBuilder.build();
        if (meshData == null) {
            close();
            return;
        }
        try (meshData) {
            close();
            vertexBuffer = RenderSystem.getDevice().createBuffer(() -> "Multi Color Chunk Highlight Buffer", BufferType.VERTICES, BufferUsage.STATIC_WRITE, meshData.vertexBuffer());
            indexCount = meshData.drawState().indexCount();
        }
    }

    @Override
    public void render(DrawContext ctx, Long2LongMap highlights, int color) {
        if (vertexBuffer == null || vertexBuffer.isClosed()) return;
        var autoIndexBuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        var indexType = autoIndexBuffer.type();
        var indexBuffer = autoIndexBuffer.getBuffer(indexCount);
        try (final RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
            .createRenderPass(Minecraft.getInstance().getMainRenderTarget().getColorTexture(),
                OptionalInt.empty())) {
            pass.setPipeline(XaeroPlusShaders.MULTI_COLOR_HIGHLIGHT_PIPELINE);
            pass.setUniform("MapViewMatrix", ctx.untranslatedMapViewMatrix());
            pass.setUniform("CameraChunk", (float) Math.floorDiv(ctx.cameraBlockX(), 16), (float) Math.floorDiv(ctx.cameraBlockZ(), 16));
            pass.setUniform("CameraInChunk", (float) Math.floorMod(ctx.cameraBlockX(), 16), (float) Math.floorMod(ctx.cameraBlockZ(), 16));
            pass.setUniform("ModelViewMat", RenderSystem.getModelViewMatrix());
            pass.setUniform("ProjMat", RenderSystem.getProjectionMatrix());
            pass.setIndexBuffer(indexBuffer, indexType);
            pass.setVertexBuffer(0, vertexBuffer);
            pass.drawIndexed(0, indexCount);
        }
    }
}
