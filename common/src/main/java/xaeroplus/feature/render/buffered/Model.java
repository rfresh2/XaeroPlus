package xaeroplus.feature.render.buffered;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

/**
 * Significant inspiration and code present has been adapted from: https://github.com/tr7zw/Exordium
 */
public class Model {
    VertexBuffer toDraw;

    public Model(Vector3f[] modelData, Vector2f[] uvData) {

        ByteBufferBuilder byteBufferBuilder = new ByteBufferBuilder(modelData.length * 4 * 5);
        BufferBuilder bufferbuilder = new BufferBuilder(byteBufferBuilder, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        for (int i = 0; i < modelData.length; i++) {
            Vector3f pos = modelData[i];
            Vector2f uv = uvData[i];
            bufferbuilder.addVertex(pos.x(), pos.y(), pos.z()).setUv(uv.x(), uv.y());
        }
        toDraw = new VertexBuffer(VertexBuffer.Usage.STATIC);
        MeshData meshData = bufferbuilder.build();
        if (meshData != null) {
            upload(meshData);
        }
    }

    public void drawWithShader(Matrix4f matrix4f, Matrix4f matrix4f2, ShaderInstance shaderInstance) {
        toDraw.bind();
        toDraw.drawWithShader(matrix4f, matrix4f2, shaderInstance);
    }

    public void draw(Matrix4f matrix4f) {
        drawWithShader(matrix4f, RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
    }

    private void upload(MeshData renderedBuffer) {
        RenderSystem.assertOnRenderThread();
        toDraw.bind();
        toDraw.upload(renderedBuffer);
    }

    public void close() {
        toDraw.close();
    }
}
