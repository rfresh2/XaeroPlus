package xaeroplus.feature.render.ellipse;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
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

public abstract class AbstractEllipseDrawFeature<T> implements DrawFeature {
    public final AsyncLoadingCache<Long, T> ellipseRenderCache;

    protected AbstractEllipseDrawFeature(final int refreshIntervalMs) {
        ellipseRenderCache = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .refreshAfterWrite(refreshIntervalMs, TimeUnit.MILLISECONDS)
            .executor(TickTaskExecutor.INSTANCE)
            .removalListener((key, value, cause) -> markDrawBufferStale())
            .buildAsync(key -> loadEllipsesInWindow());
    }

    @Override
    public void invalidateCache() {
        ellipseRenderCache.synchronous().invalidateAll();
        markDrawBufferStale();
    }

    public abstract float thickness();

    public T loadEllipsesInWindow() {
        var window = MapRenderWindow.resolveCurrent();
        return preProcessEllipses(
            provideEllipsesInWindow(window.windowX(), window.windowZ(), window.windowSize(), window.dimension()),
            window.windowX(),
            window.windowZ(),
            window.windowSize()
        );
    }

    public abstract T provideEllipsesInWindow(int windowX, int windowZ, int windowSize, ResourceKey<Level> dimension);

    public abstract T preProcessEllipses(T ellipses, int windowX, int windowZ, int windowSize);

    public abstract T emptyEllipses();

    protected abstract void markDrawBufferStale();

    protected abstract void closeDrawBuffer();

    public T getEllipses() {
        return ellipseRenderCache.get(0L).getNow(emptyEllipses());
    }

    protected float thicknessScale(final DrawContext ctx) {
        return 16.0f * (float) Mth.clamp(
            thickness() * ctx.fboScale(),
            0.1f * (ctx.worldmap() ? 1.0f : Globals.minimapScaleMultiplier),
            1000.0f
        );
    }

    public void preRender(final DrawContext ctx) {
        if (ctx.worldmap()) {
            XaeroPlusShaders.setEllipsesFrameSize(Minecraft.getInstance().getWindow().getWidth(), Minecraft.getInstance().getWindow().getHeight());
        }
    }

    public void postRender(final DrawContext ctx) {}

    @Override
    public void close() {
        ellipseRenderCache.synchronous().invalidateAll();
        closeDrawBuffer();
    }
}
