package xaeroplus.feature.render.buffered;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

/**
 * Significant inspiration and code present has been adapted from: https://github.com/tr7zw/Exordium
 */
public class Model {
    private final VertexBuffer vertexBuffer;

    public Model(final Vector3f[] posMatrix, final Vector2f[] texUvMatrix) {
        var bufferbuilder = Tesselator.getInstance()
            .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        for (int i = 0; i < posMatrix.length; i++) {
            var pos = posMatrix[i];
            var uv = texUvMatrix[i];
            bufferbuilder.addVertex(pos.x(), pos.y(), pos.z()).setUv(uv.x(), uv.y());
        }
        vertexBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        RenderSystem.assertOnRenderThread();
        var renderedBuffer = bufferbuilder.build();
        if (renderedBuffer != null) {
            vertexBuffer.bind();
            vertexBuffer.upload(renderedBuffer);
        }
    }

    public void draw(final Matrix4f modelViewMatrix) {
        vertexBuffer.bind();
        vertexBuffer.drawWithShader(modelViewMatrix, RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
    }

    public void close() {
        vertexBuffer.close();
    }
}
