package xaeroplus.feature.render.line;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import xaeroplus.feature.render.DrawContext;
import xaeroplus.feature.render.DrawHelper;
import xaeroplus.util.ColorHelper;

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
        var bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
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
        if (vertexBuffer == null || vertexBuffer.isInvalid()) {
            close();
            vertexBuffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
        }
        var meshData = bufferBuilder.end();
        vertexBuffer.bind();
        vertexBuffer.upload(meshData);
    }
}
