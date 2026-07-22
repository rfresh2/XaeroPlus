package xaeroplus.feature.render.ellipse;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import xaeroplus.feature.render.DrawContext;
import xaeroplus.feature.render.DrawHelper;
import xaeroplus.util.ColorHelper;

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
        var bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
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
        if (vertexBuffer == null || vertexBuffer.isInvalid()) {
            close();
            vertexBuffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
        }
        var meshData = bufferBuilder.end();
        vertexBuffer.bind();
        vertexBuffer.upload(meshData);
    }
}
