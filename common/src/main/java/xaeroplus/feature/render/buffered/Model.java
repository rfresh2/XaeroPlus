package xaeroplus.feature.render.buffered;

import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Vector2f;
import org.joml.Vector3f;

/**
 * Significant inspiration and code present has been adapted from: https://github.com/tr7zw/Exordium
 */
public class Model {
    private final GpuBuffer vertexBuffer;

    public Model(final Vector3f[] posMatrix, final Vector2f[] texUvMatrix) {
        RenderSystem.assertOnRenderThread();
        var bufferbuilder = Tesselator.getInstance()
            .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        for (int i = 0; i < posMatrix.length; i++) {
            var pos = posMatrix[i];
            var uv = texUvMatrix[i];
            bufferbuilder.addVertex(pos.x(), pos.y(), pos.z()).setUv(uv.x(), uv.y());
        }
        try (var renderedBuffer = bufferbuilder.buildOrThrow()) {
            this.vertexBuffer = RenderSystem.getDevice().createBuffer(() -> "XaeroPlus Buffered Minimap Model", BufferType.VERTICES, BufferUsage.STATIC_WRITE, renderedBuffer.vertexBuffer());
        }
    }

    public void draw(RenderPass renderPass) {
        renderPass.setVertexBuffer(0, vertexBuffer);
        renderPass.draw(0, vertexBuffer.size());
    }

    public void close() {
        vertexBuffer.close();
    }
}
