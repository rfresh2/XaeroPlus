package xaeroplus.feature.render.highlight;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexBuffer;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import net.minecraft.client.renderer.ShaderInstance;
import org.jetbrains.annotations.Nullable;
import xaeroplus.feature.render.DrawContext;

public abstract class AbstractHighlightVertexBuffer {
    protected boolean stale = true;
    @Nullable protected VertexBuffer vertexBuffer = null;
    protected boolean flipped = false;
    public long lastRefreshed = 0L;

    public boolean needsRefresh(DrawContext ctx) {
        return vertexBuffer == null || vertexBuffer.isInvalid() || stale || flipped != ctx.worldmap();
    }

    public abstract ShaderInstance shaderInstance();

    public void preRender(final DrawContext ctx, final Long2LongMap highlights, final int color) {
        if (needsRefresh(ctx)) {
            refresh(ctx, highlights, color);
        }
    }

    public abstract void refresh(DrawContext ctx, Long2LongMap highlights, int color);

    public void render() {
        if (vertexBuffer == null || vertexBuffer.isInvalid()) return;
        var shader = shaderInstance();
        if (shader == null) return;
        vertexBuffer.bind();
        vertexBuffer.drawWithShader(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), shader);
    }

    public void markStale() { stale = true; }

    public void close() {
        if (vertexBuffer != null) {
            vertexBuffer.close();
            vertexBuffer = null;
        }
    }
}
