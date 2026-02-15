package xaeroplus.feature.render.line;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import xaeroplus.Globals;
import xaeroplus.feature.render.DrawContext;
import xaeroplus.feature.render.DrawFeature;
import xaeroplus.feature.render.MapRenderWindow;
import xaeroplus.feature.render.shaders.XaeroPlusShaders;
import xaeroplus.module.impl.TickTaskExecutor;

import java.util.concurrent.TimeUnit;

public abstract class AbstractLineDrawFeature<T> implements DrawFeature {
    public final AsyncLoadingCache<Long, T> lineRenderCache;

    protected AbstractLineDrawFeature(int refreshIntervalMs) {
        this.lineRenderCache = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .refreshAfterWrite(refreshIntervalMs, TimeUnit.MILLISECONDS)
            .executor(TickTaskExecutor.INSTANCE)
            .removalListener((k, v, cause) -> markDrawBufferStale())
            .buildAsync(k -> loadLinesInWindow());
    }

    @Override
    public void invalidateCache() {
        lineRenderCache.synchronous().invalidateAll();
        markDrawBufferStale();
    }

    public abstract float lineWidth();

    public T loadLinesInWindow() {
        var window = MapRenderWindow.resolveCurrent();
        return preProcessLines(
            provideLinesInWindow(window.windowX(), window.windowZ(), window.windowSize(), window.dimension()),
            window.windowX(),
            window.windowZ(),
            window.windowSize()
        );
    }

    public abstract T provideLinesInWindow(int windowX, int windowZ, int windowSize, ResourceKey<Level> dimension);

    public abstract T preProcessLines(T lines, final int windowX, final int windowZ, final int windowSize);

    public abstract T emptyLines();

    protected abstract void markDrawBufferStale();

    protected abstract void closeDrawBuffer();

    public T getLines() {
        return lineRenderCache.get(0L).getNow(emptyLines());
    }

    public void preRender(DrawContext ctx) {
        XaeroPlusShaders.ensureShaders();
        var mc = Minecraft.getInstance();
        if (ctx.worldmap()) {
            XaeroPlusShaders.LINES_SHADER.setFrameSize(mc.getWindow().getWidth(), mc.getWindow().getHeight());
        }
        float lineWidthScale = 16f * (float) Mth.clamp(
            lineWidth() * ctx.fboScale(),
            0.1f * (ctx.worldmap() ? 1.0f : Globals.minimapScaleMultiplier),
            1000.0f
        );
//        RenderSystem.lineWidth(lineWidthScale);
        // VertexBuffer._drawWithShader() only updates line width uniform if mode is set to LINES or LINE_STRIP
        // so we have to set it ourselves
        XaeroPlusShaders.LINES_SHADER.LINE_WIDTH.set(lineWidthScale);
        XaeroPlusShaders.LINES_SHADER.setMapViewMatrix(ctx.matrixStack().last().pose());
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
    }

    public void postRender(DrawContext ctx) {
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
    }

    @Override
    public void close() {
        lineRenderCache.synchronous().invalidateAll();
        closeDrawBuffer();
    }
}
