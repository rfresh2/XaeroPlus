package xaeroplus.feature.render.line;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import xaeroplus.feature.render.DrawContext;
import xaeroplus.feature.render.DrawHelper;
import xaeroplus.util.ColorHelper;

import java.util.List;

public class LineVertexBuffer extends AbstractLineVertexBuffer<List<Line>> {
    private int color = -1;

    public void setColor(final int color) {
        if (this.color != color) {
            this.color = color;
            markStale();
        }
    }

    @Override
    protected void refresh(final DrawContext ctx, final List<Line> lines) {
        stale = false;
        flipped = ctx.worldmap();
        if (lines.isEmpty() || ColorHelper.getA(color) == 0.0f) {
            close();
            return;
        }
        var r = ColorHelper.getR(color);
        var g = ColorHelper.getG(color);
        var b = ColorHelper.getB(color);
        var a = ColorHelper.getA(color);
        var bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        for (int i = 0; i < lines.size(); i++) {
            var line = lines.get(i);
            int x1 = flipped ? line.x2() : line.x1();
            int z1 = flipped ? line.z2() : line.z1();
            int x2 = flipped ? line.x1() : line.x2();
            int z2 = flipped ? line.z1() : line.z2();
            DrawHelper.addColoredLineQuadToExistingBuffer(bufferBuilder, x1, z1, x2, z2, r, g, b, a);
        }
        if (vertexBuffer == null || vertexBuffer.isInvalid()) {
            close();
            vertexBuffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
        }
        var meshData = bufferBuilder.end();
        vertexBuffer.bind();
        vertexBuffer.upload(meshData);
    }
}
