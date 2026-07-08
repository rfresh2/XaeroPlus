package xaeroplus.feature.render;

import com.mojang.blaze3d.vertex.VertexConsumer;

public class DrawHelper {

    public static void addColoredLineQuadToExistingBuffer(
        VertexConsumer vertexBuffer, float x1, float y1, float x2, float y2, float r, float g, float b, float a
    ) {
        // The line shader expands this segment into a quad in screen space using gl_VertexID.
        // Position and UV0 are both in map-space and transformed by MapViewMatrix in the shader.
        vertexBuffer.addVertex(x1, y1, 0.0F).setUv(x2, y2).setColor(r, g, b, a);
        vertexBuffer.addVertex(x1, y1, 0.0F).setUv(x2, y2).setColor(r, g, b, a);
        vertexBuffer.addVertex(x1, y1, 0.0F).setUv(x2, y2).setColor(r, g, b, a);
        vertexBuffer.addVertex(x1, y1, 0.0F).setUv(x2, y2).setColor(r, g, b, a);
    }
}
