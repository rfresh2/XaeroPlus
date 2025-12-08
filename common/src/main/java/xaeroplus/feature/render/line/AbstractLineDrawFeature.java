package xaeroplus.feature.render.line;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import xaeroplus.Globals;
import xaeroplus.feature.render.DrawContext;
import xaeroplus.feature.render.DrawFeature;
import xaeroplus.feature.render.shaders.XaeroPlusShaders;
import xaeroplus.module.impl.TickTaskExecutor;
import xaeroplus.util.ChunkUtils;

import java.util.OptionalInt;
import java.util.concurrent.TimeUnit;

import static xaeroplus.util.GuiMapHelper.*;

public abstract class AbstractLineDrawFeature<T> implements DrawFeature {
    public final AsyncLoadingCache<Long, T> lineRenderCache;

    protected AbstractLineDrawFeature(int refreshIntervalMs) {
        this.lineRenderCache = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .refreshAfterWrite(refreshIntervalMs, TimeUnit.MILLISECONDS)
            .executor(TickTaskExecutor.INSTANCE)
            .buildAsync(k -> loadLinesInWindow());
    }

    @Override
    public void invalidateCache() {
        lineRenderCache.synchronous().invalidateAll();
    }

    public abstract float lineWidth();

    public T loadLinesInWindow() {
        final int windowX, windowZ, windowSize;
        var guiMapOptional = getGuiMap();
        if (guiMapOptional.isPresent()) {
            var guiMap = guiMapOptional.get();
            windowX = getGuiMapCenterRegionX(guiMap);
            windowZ = getGuiMapCenterRegionZ(guiMap);
            windowSize = getGuiMapRegionSize(guiMap);
        } else {
            windowX = ChunkUtils.getPlayerRegionX();
            windowZ = ChunkUtils.getPlayerRegionZ();
            windowSize = Math.max(3, Globals.minimapScaleMultiplier);
        }
        return preProcessLines(provideLinesInWindow(windowX, windowZ, windowSize, Globals.getCurrentDimensionId()), windowX, windowZ, windowSize);
    }

    public abstract T provideLinesInWindow(int windowX, int windowZ, int windowSize, ResourceKey<Level> dimension);

    public abstract T preProcessLines(T lines, final int windowX, final int windowZ, final int windowSize);

    public abstract T emptyLines();

    public T getLines() {
        return lineRenderCache.get(0L).getNow(emptyLines());
    }

    public void preRender(DrawContext ctx) {
        if (ctx.worldmap()) {
            XaeroPlusShaders.setLinesFrameSize(Minecraft.getInstance().getWindow().getWidth(), Minecraft.getInstance().getWindow().getHeight());
        }
    }

    void drawLines(DrawContext ctx, MeshData meshData) {
        VertexFormat.IndexType indexType;
        GpuBuffer indexBuffer;
        GpuBuffer vertexBuffer;
        try (meshData) {
            if (meshData.indexBuffer() == null) {
                RenderSystem.AutoStorageIndexBuffer autoStorageIndexBuffer = RenderSystem.getSequentialBuffer(meshData.drawState().mode());
                indexBuffer = autoStorageIndexBuffer.getBuffer(meshData.drawState().indexCount());
                indexType = autoStorageIndexBuffer.type();
            } else {
                indexBuffer = XaeroPlusShaders.LINES_PIPELINE.getVertexFormat().uploadImmediateIndexBuffer(meshData.indexBuffer());
                indexType = meshData.drawState().indexType();
            }
            vertexBuffer = XaeroPlusShaders.LINES_PIPELINE.getVertexFormat().uploadImmediateVertexBuffer(meshData.vertexBuffer());
        }
        try (var pass = RenderSystem.getDevice().createCommandEncoder()
            .createRenderPass(Minecraft.getInstance().getMainRenderTarget().getColorTexture(), OptionalInt.empty())) {
            pass.setPipeline(XaeroPlusShaders.LINES_PIPELINE);
            pass.setUniform("ModelViewMatrix", RenderSystem.getModelViewMatrix());
            pass.setUniform("ProjMat", RenderSystem.getProjectionMatrix());
            pass.setUniform("FrameSize", XaeroPlusShaders.LINES_FRAME_SIZE);
            pass.setUniform("ColorModulator", RenderSystem.getShaderColor());
            float lineWidthScale = 16f * (float) Mth.clamp(
                lineWidth() * ctx.fboScale(),
                0.1f * (ctx.worldmap() ? 1.0f : Globals.minimapScaleMultiplier),
                1000.0f
            );
            pass.setUniform("LineWidth", lineWidthScale);
            pass.setIndexBuffer(indexBuffer, indexType);
            pass.setVertexBuffer(0, vertexBuffer);
            pass.drawIndexed(0, meshData.drawState().indexCount());
        }
    }

    public void postRender(DrawContext ctx) {}

    @Override
    public void close() {
        lineRenderCache.synchronous().invalidateAll();
    }
}
