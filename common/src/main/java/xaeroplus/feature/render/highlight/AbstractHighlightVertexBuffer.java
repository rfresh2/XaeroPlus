package xaeroplus.feature.render.highlight;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import net.minecraft.client.renderer.MappableRingBuffer;
import org.jetbrains.annotations.Nullable;
import xaeroplus.feature.render.DrawContext;

public abstract class AbstractHighlightVertexBuffer {
    protected boolean stale = true;
    @Nullable protected GpuBuffer vertexBuffer = null;
    protected boolean flipped = false;
    public long lastRefreshed = 0L;
    public int indexCount = 0;
    public static final MappableRingBuffer uniformBuffer = new MappableRingBuffer(
        () -> "XaeroPlus Highlight Uniform Buffer",
        GpuBuffer.USAGE_MAP_WRITE,
        new Std140SizeCalculator()
            .putMat4f()
            .putMat4f()
            .putMat4f()
            .putVec4()
            .get()
    );

    public boolean needsRefresh(DrawContext ctx) {
        return vertexBuffer == null || vertexBuffer.isClosed() || stale || flipped != ctx.worldmap();
    }

    public void preRender(final DrawContext ctx, final Long2LongMap highlights, final int color) {
        if (needsRefresh(ctx)) {
            refresh(ctx, highlights, color);
        }
    }

    public abstract void refresh(DrawContext ctx, Long2LongMap highlights, int color);

    public abstract void render(DrawContext ctx, Long2LongMap highlights, int color);

    public void markStale() { stale = true; }

    public void close() {
        if (vertexBuffer != null) {
            vertexBuffer.close();
            vertexBuffer = null;
        }
    }
}
