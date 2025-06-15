package xaeroplus.feature.render.line;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import xaero.common.graphics.shader.MinimapShaders;
import xaeroplus.Globals;
import xaeroplus.feature.render.DrawContext;
import xaeroplus.feature.render.DrawFeature;
import xaeroplus.module.ModuleManager;
import xaeroplus.module.impl.TickTaskExecutor;
import xaeroplus.util.ChunkUtils;

import java.util.concurrent.TimeUnit;

import static xaeroplus.util.GuiMapHelper.*;

public abstract class AbstractLineDrawFeature<T> implements DrawFeature {
    public final AsyncLoadingCache<Long, T> lineRenderCache;

    protected AbstractLineDrawFeature(int refreshIntervalMs) {
        this.lineRenderCache = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .refreshAfterWrite(refreshIntervalMs, TimeUnit.MILLISECONDS)
            .executor(ModuleManager.getModule(TickTaskExecutor.class))
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
        return preProcessLines(provideLinesInWindow(windowX, windowZ, windowSize, Globals.getCurrentDimensionId()));
    }

    public abstract T provideLinesInWindow(int windowX, int windowZ, int windowSize, ResourceKey<Level> dimension);

    public abstract T preProcessLines(T lines);

    public abstract T emptyLines();

    public T getLines() {
        return lineRenderCache.get(0L).getNow(emptyLines());
    }

    public void preRender(DrawContext ctx) {
        MinimapShaders.ensureShaders();
        var mc = Minecraft.getInstance();
        if (ctx.worldmap()) {
            MinimapShaders.FRAMEBUFFER_LINES.setFrameSize(mc.getWindow().getWidth(), mc.getWindow().getHeight());
        }
        float lineWidthScale = 16f * (float) Mth.clamp(
            lineWidth() * ctx.fboScale(),
            0.1f * (ctx.worldmap() ? 1.0f : Globals.minimapScaleMultiplier),
            1000.0f
        );
        RenderSystem.lineWidth(lineWidthScale);
    }

    @Override
    public void close() {
        lineRenderCache.synchronous().invalidateAll();
    }
}
