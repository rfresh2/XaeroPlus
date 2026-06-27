package xaeroplus.feature.render.line;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import net.minecraft.client.renderer.MappableRingBuffer;
import org.jetbrains.annotations.Nullable;
import xaeroplus.feature.render.DrawContext;

public abstract class AbstractLineVertexBuffer<T> {
    protected boolean stale = true;
    @Nullable protected GpuBuffer vertexBuffer = null;
    protected boolean flipped = false;
    protected int indexCount = 0;
    public MappableRingBuffer uniformBuffer = null;
    public MappableRingBuffer dynamicTransformUniformBuffer = null;

    public boolean needsRefresh(final DrawContext ctx) {
        return vertexBuffer == null || vertexBuffer.isClosed() || stale || flipped != ctx.worldmap() || uniformBuffer == null;
    }

    public void preRender(final DrawContext ctx, final T lines) {
        if (needsRefresh(ctx)) {
            refresh(ctx, lines);
        }
        if (uniformBuffer == null) {
            uniformBuffer = new MappableRingBuffer(
                () -> "XaeroPlus Lines Uniform Buffer",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_MAP_WRITE,
                new Std140SizeCalculator()
                    .putMat4f()
                    .putVec2()
                    .putFloat()
                    .align(80)
                    .get()
            );
        }
        if (dynamicTransformUniformBuffer == null) {
            dynamicTransformUniformBuffer = new MappableRingBuffer(
                () -> "XaeroPlus Lines DynamicTransforms Uniform Buffer",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_MAP_WRITE,
                new Std140SizeCalculator()
                    .putMat4f()
                    .putVec4()
                    .putVec3()
                    .putMat4f()
                    .get()
            );
        }
    }

    protected abstract void refresh(DrawContext ctx, T lines);

    public abstract void render(DrawContext ctx, float lineWidthScale);

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
        if (dynamicTransformUniformBuffer != null) {
            dynamicTransformUniformBuffer.close();
            dynamicTransformUniformBuffer = null;
        }
    }
}
