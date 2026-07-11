package xaeroplus.feature.render;

import com.mojang.blaze3d.vertex.VertexConsumer;

public class DrawHelper {

    public static void addColoredLineQuadToExistingBuffer(
        VertexConsumer vertexBuffer, float x1, float y1, float x2, float y2, float r, float g, float b, float a
    ) {
        // The line shader expands this segment into a quad in screen space using gl_VertexID.
        // Position and UV0 are relative to the line buffer's block origin. The shader moves them
        // into camera-relative map-space before applying MapViewMatrix.
        vertexBuffer.vertex(x1, y1, 0.0F).color(r, g, b, a).uv(x2, y2).endVertex();
        vertexBuffer.vertex(x1, y1, 0.0F).color(r, g, b, a).uv(x2, y2).endVertex();
        vertexBuffer.vertex(x1, y1, 0.0F).color(r, g, b, a).uv(x2, y2).endVertex();
        vertexBuffer.vertex(x1, y1, 0.0F).color(r, g, b, a).uv(x2, y2).endVertex();
    }
}
