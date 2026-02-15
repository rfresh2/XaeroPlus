package xaeroplus.feature.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.renderer.ShaderInstance;
import org.jetbrains.annotations.Nullable;

public abstract class CachedVertexBuffer {
    protected boolean stale = true;
    @Nullable protected VertexBuffer vertexBuffer = null;
    protected boolean flipped = false;

    public boolean needsRefresh(final DrawContext ctx) {
        return vertexBuffer == null || vertexBuffer.isInvalid() || stale || flipped != ctx.worldmap();
    }

    protected abstract ShaderInstance shaderInstance();

    public void render() {
        if (vertexBuffer == null || vertexBuffer.isInvalid()) return;
        var shader = shaderInstance();
        if (shader == null) return;
        vertexBuffer.bind();
        vertexBuffer.drawWithShader(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), shader);
    }

    public void markStale() {
        stale = true;
    }

    public void close() {
        if (vertexBuffer != null) {
            vertexBuffer.close();
            vertexBuffer = null;
        }
    }
}
