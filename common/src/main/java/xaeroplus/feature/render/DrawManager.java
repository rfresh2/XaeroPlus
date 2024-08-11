package xaeroplus.feature.render;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.lenni0451.lambdaevents.EventHandler;
import xaero.common.minimap.render.MinimapRendererHelper;
import xaero.map.gui.GuiMap;
import xaeroplus.Globals;
import xaeroplus.XaeroPlus;
import xaeroplus.event.DimensionSwitchEvent;
import xaeroplus.event.XaeroWorldChangeEvent;
import xaeroplus.util.ChunkUtils;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static xaeroplus.util.GuiMapHelper.*;

public class DrawManager {
    private final Reference2ObjectMap<Class<?>, WindowedDrawFeature> chunkHighlightDrawFeatures = new Reference2ObjectOpenHashMap<>();

    public DrawManager() {
        XaeroPlus.EVENT_BUS.register(this);
    }

    public synchronized void registerChunkHighlightProvider(Class<?> clazz, ChunkHighlightProvider feature) {
        chunkHighlightDrawFeatures.put(clazz, new WindowedDrawFeature(feature, createChunkHighlightRenderCache(feature)));
    }

    @EventHandler
    public void onDimensionChange(DimensionSwitchEvent event) {
        chunkHighlightDrawFeatures.values().forEach(feature -> {
            feature.chunkRenderCache().synchronous().invalidateAll();
        });
    }

    @EventHandler
    public void onXaeroWorldChange(XaeroWorldChangeEvent event) {
        chunkHighlightDrawFeatures.values().forEach(feature -> {
            feature.chunkRenderCache().synchronous().invalidateAll();
        });
    }

    public synchronized void unregister(Class<?> clazz) {
        chunkHighlightDrawFeatures.remove(clazz);
    }

    private AsyncLoadingCache<Long, LongSet> createChunkHighlightRenderCache(final ChunkHighlightProvider chunkHighlightProvider) {
        return Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .refreshAfterWrite(500, TimeUnit.MILLISECONDS)
            .executor(Globals.cacheRefreshExecutorService.get())
            // only one key
            .buildAsync(k -> loadFeatureHighlightsInWindow(chunkHighlightProvider));
    }

    private LongSet loadFeatureHighlightsInWindow(final ChunkHighlightProvider chunkHighlightProvider) {
        int windowX;
        int windowZ;
        int windowSize;
        Optional<GuiMap> guiMapOptional = getGuiMap();
        if (guiMapOptional.isPresent()) {
            final GuiMap guiMap = guiMapOptional.get();
            windowX = getGuiMapCenterRegionX(guiMap);
            windowZ = getGuiMapCenterRegionZ(guiMap);
            windowSize = getGuiMapRegionSize(guiMap);
        } else {
            windowX = ChunkUtils.getPlayerRegionX();
            windowZ = ChunkUtils.getPlayerRegionZ();
            windowSize = Math.max(3, Globals.minimapScaleMultiplier);
        }
        return chunkHighlightProvider.chunkHighlightSupplier().getHighlights(windowX, windowZ, windowSize, Globals.getCurrentDimensionId());
    }

    public synchronized void drawMinimapFeatures(
        int minViewX,
        int maxViewX,
        int minViewZ,
        int maxViewZ,
        int chunkX,
        int chunkZ,
        int tileX,
        int tileZ,
        int insideX,
        int insideZ,
        final PoseStack matrixStack,
        final VertexConsumer overlayBufferBuilder,
        MinimapRendererHelper helper
    ) {
        if (chunkHighlightDrawFeatures.isEmpty()) return;
        for (WindowedDrawFeature feature : chunkHighlightDrawFeatures.values()) {
            int color = feature.chunkHighlightProvider().colorSupplier().getAsInt();
            float a = ((color >> 24) & 255) / 255.0f;
            if (a == 0.0f) return;
            var highlights = feature.getChunkHighlights();
            var it = highlights.longIterator();
            while (it.hasNext()) {
                long highlight = it.nextLong();
                var chunkPosX = ChunkUtils.longToChunkX(highlight);
                var chunkPosZ = ChunkUtils.longToChunkZ(highlight);
                var mapTileChunkX = ChunkUtils.chunkCoordToMapTileChunkCoord(chunkPosX);
                var mapTileChunkZ = ChunkUtils.chunkCoordToMapTileChunkCoord(chunkPosZ);
                if (mapTileChunkX < minViewX || mapTileChunkX > maxViewX) continue;
                if (mapTileChunkZ < minViewZ || mapTileChunkZ > maxViewZ) continue;
                var regionX = ChunkUtils.chunkCoordToRegionCoord(chunkPosX);
                var regionZ = ChunkUtils.chunkCoordToRegionCoord(chunkPosZ);
                var cx = regionX & 7;
                var cz = regionZ & 7;
                var drawX = ((cx - chunkX) << 6) - (tileX << 4) - insideX;
                var drawZ = ((cz - chunkZ) << 6) - (tileZ << 4) - insideZ;
                var left = drawX + 16 * (chunkPosX - cx * 4);
                var top = drawZ + 16 * (chunkPosZ - cz * 4);
                helper.addColoredRectToExistingBuffer(
                    matrixStack.last().pose(),
                    overlayBufferBuilder,
                    left,
                    top,
                    16,
                    16,
                    color);
            }
        }
    }

    public synchronized void drawWorldMapFeatures(
        final int minRegX,
        final int maxRegX,
        final int minRegZ,
        final int maxRegZ,
        final int flooredCameraX,
        final int flooredCameraZ,
        final PoseStack matrixStack,
        final VertexConsumer overlayBuffer
    ) {
        for (WindowedDrawFeature feature : chunkHighlightDrawFeatures.values()) {
            int color = feature.chunkHighlightProvider().colorSupplier().getAsInt();
            float a = ((color >> 24) & 255) / 255.0f;
            if (a == 0.0f) return;
            float r = (float)(color >> 16 & 255) / 255.0F;
            float g = (float)(color >> 8 & 255) / 255.0F;
            float b = (float)(color & 255) / 255.0F;
            var highlights = feature.getChunkHighlights();
            var it = highlights.longIterator();
            while (it.hasNext()) {
                long highlight = it.nextLong();
                var chunkPosX = ChunkUtils.longToChunkX(highlight);
                var chunkPosZ = ChunkUtils.longToChunkZ(highlight);
                var regionX = ChunkUtils.chunkCoordToMapRegionCoord(chunkPosX);
                var regionZ = ChunkUtils.chunkCoordToMapRegionCoord(chunkPosZ);
                if (regionX < minRegX || regionX > maxRegX) continue;
                if (regionZ < minRegZ || regionZ > maxRegZ) continue;
                final float left = (float) (ChunkUtils.chunkCoordToCoord(chunkPosX) - flooredCameraX);
                final float top = (float) (ChunkUtils.chunkCoordToCoord(chunkPosZ) - flooredCameraZ);
                final float right = left + 16;
                final float bottom = top + 16;
                MinimapBackgroundDrawHelper.fillIntoExistingBuffer(
                    matrixStack.last().pose(),
                    overlayBuffer,
                    left,
                    top,
                    right,
                    bottom,
                    r,
                    g,
                    b,
                    a);
            }
        }
    }
}
