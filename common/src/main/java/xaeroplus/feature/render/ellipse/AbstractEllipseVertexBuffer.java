package xaeroplus.feature.render.ellipse;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import net.minecraft.client.renderer.MappableRingBuffer;
import org.jetbrains.annotations.Nullable;
import xaeroplus.feature.render.DrawContext;

public abstract class AbstractEllipseVertexBuffer<T> {
    protected boolean stale = true;
    @Nullable protected GpuBuffer vertexBuffer = null;
    protected boolean flipped = false;
    protected int indexCount = 0;
    public MappableRingBuffer uniformBuffer = null;
    protected int bufferOriginBlockX;
    protected int bufferOriginBlockZ;

    public boolean needsRefresh(final DrawContext ctx) {
        return vertexBuffer == null || vertexBuffer.isClosed() || stale || flipped != ctx.worldmap() || uniformBuffer == null;
    }

    public void preRender(final DrawContext ctx, final T ellipses) {
        if (needsRefresh(ctx)) {
            refresh(ctx, ellipses);
        }
        if (uniformBuffer == null) {
            uniformBuffer = new MappableRingBuffer(
                () -> "XaeroPlus Ellipses Uniform Buffer",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_MAP_WRITE,
                new Std140SizeCalculator()
                    .putMat4f()
                    .putVec2()
                    .putVec2()
                    .get()
            );
        }
    }

    protected void setBufferOrigin(final DrawContext ctx) {
        bufferOriginBlockX = ctx.cameraBlockX();
        bufferOriginBlockZ = ctx.cameraBlockZ();
    }

    protected abstract void refresh(DrawContext ctx, T ellipses);

    public abstract void render(DrawContext ctx, float thicknessScale);

    public void markStale() {
        stale = true;
    }

    public void close() {
        if (vertexBuffer != null) {
            vertexBuffer.close();
            vertexBuffer = null;
        }
        if (uniformBuffer != null) {
            uniformBuffer.close();
            uniformBuffer = null;
        }
    }
}
