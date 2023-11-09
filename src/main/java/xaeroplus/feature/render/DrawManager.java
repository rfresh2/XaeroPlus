package xaeroplus.feature.render;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongList;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import xaero.common.minimap.render.MinimapRendererHelper;
import xaeroplus.Globals;
import xaeroplus.util.ChunkUtils;

import java.util.IdentityHashMap;
import java.util.Map;

public class DrawManager {
    private final Map<Class<?>, DrawFeature> chunkHighlightDrawFeatures = new IdentityHashMap<>();

    public synchronized void registerChunkHighlightProvider(Class<?> clazz, ChunkHighlightProvider feature) {
        chunkHighlightDrawFeatures.put(clazz, new DrawFeature(
            feature,
            createChunkHighlightRenderCache(feature)));
    }

    public synchronized void unregister(Class<?> clazz) {
        chunkHighlightDrawFeatures.remove(clazz);
    }

    private AsyncLoadingCache<Long, LongList> createChunkHighlightRenderCache(final ChunkHighlightProvider chunkHighlightProvider) {
        return Caffeine.newBuilder()
            .expireAfterWrite(3000, java.util.concurrent.TimeUnit.MILLISECONDS)
            .refreshAfterWrite(500, java.util.concurrent.TimeUnit.MILLISECONDS)
            .executor(Globals.cacheRefreshExecutorService.get())
            .buildAsync(regionLong -> loadHighlightChunksInRegion(regionLong, chunkHighlightProvider.chunkHighlightPredicate()));
    }

    private LongList loadHighlightChunksInRegion(final long regionLong, final ChunkHighlightPredicate highlightPredicate) {
        final LongList list = new LongArrayList(8);
        final int regionX = ChunkUtils.longToChunkX(regionLong);
        final int regionZ = ChunkUtils.longToChunkZ(regionLong);
        for (int cx = 0; cx < 8; cx++) {
            for (int cz = 0; cz < 8; cz++) {
                final int mapTileChunkX = (regionX << 3) + cx;
                final int mapTileChunkZ = (regionZ << 3) + cz;
                for (int t = 0; t < 16; ++t) {
                    final int chunkPosX = (mapTileChunkX << 2) + t % 4;
                    final int chunkPosZ = (mapTileChunkZ << 2) + (t >> 2);
                    if (highlightPredicate.isHighlighted(chunkPosX, chunkPosZ, Globals.customDimensionId)) {
                        list.add(ChunkUtils.chunkPosToLong(chunkPosX, chunkPosZ));
                    }
                }
            }
        }
        return list;
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
        final MatrixStack matrixStack,
        final VertexConsumer overlayBufferBuilder,
        MinimapRendererHelper helper
        ) {
        final LongArraySet regions = new LongArraySet(4);
        for (int i = minViewX; i <= maxViewX; i++) {
            for (int j = minViewZ; j <= maxViewZ; j++) {
                int regX = i >> 3;
                int regZ = j >> 3;
                regions.add(ChunkUtils.chunkPosToLong(regX, regZ));
            }
        }
        var regionsArray = regions.toLongArray();
        for (DrawFeature feature : chunkHighlightDrawFeatures.values()) {
            drawMinimapChunkHighlights(
                feature,
                regionsArray,
                chunkX,
                chunkZ,
                tileX,
                tileZ,
                insideX,
                insideZ,
                matrixStack,
                overlayBufferBuilder,
                helper
            );
        }
    }

    public synchronized void drawWorldMapFeatures(
        final int leafRegionX,
        final int leafRegionZ,
        final int level,
        final int flooredCameraX,
        final int flooredCameraZ,
        final MatrixStack matrixStack,
        final VertexConsumer overlayBuffer
    ) {
        final int mx = leafRegionX + level;
        final int mz = leafRegionZ + level;
        final LongArraySet regions = new LongArraySet(1);
        for (int regX = leafRegionX; regX < mx; ++regX) {
            for (int regZ = leafRegionZ; regZ < mz; ++regZ) {
                regions.add(ChunkUtils.chunkPosToLong(regX, regZ));
            }
        }
        final long[] regionsArray = regions.toLongArray();
        for (DrawFeature feature : chunkHighlightDrawFeatures.values()) {
            drawWorldMapChunkHighlights(
                feature,
                regionsArray,
                flooredCameraX,
                flooredCameraZ,
                matrixStack,
                overlayBuffer
            );
        }
    }


    private void drawMinimapChunkHighlights(final DrawFeature feature,
                                            final long[] regionsArray,
                                            int chunkX,
                                            int chunkZ,
                                            int tileX,
                                            int tileZ,
                                            int insideX,
                                            int insideZ,
                                            final MatrixStack matrixStack,
                                            final VertexConsumer overlayBufferBuilder,
                                            MinimapRendererHelper helper
    ) {
        int color = feature.chunkHighlightProvider().colorSupplier().get();
        float a = ((color >> 24) & 255) / 255.0f;
        if (a == 0.0f) return;
        for (int r = 0; r < regionsArray.length; r++) {
            var regionLong = regionsArray[r];
            var regionX = ChunkUtils.longToChunkX(regionLong);
            var regionZ = ChunkUtils.longToChunkZ(regionLong);
            var cx = regionX & 7;
            var cz = regionZ & 7;
            var drawX = ((cx - chunkX) << 6) - (tileX << 4) - insideX;
            var drawZ = ((cz - chunkZ) << 6) - (tileZ << 4) - insideZ;
            final LongList highlights = feature.getChunkHighlights(regionLong);
            for (int i = 0; i < highlights.size(); i++) {
                var chunkPosLong = highlights.getLong(i);
                var chunkPosX = ChunkUtils.longToChunkX(chunkPosLong);
                var chunkPosZ = ChunkUtils.longToChunkZ(chunkPosLong);
                var left = drawX + 16 * (chunkPosX - cx * 4);
                var top = drawZ + 16 * (chunkPosZ - cz * 4);
                helper.addColoredRectToExistingBuffer(
                    matrixStack.peek().getPositionMatrix(),
                    overlayBufferBuilder,
                    left,
                    top,
                    16,
                    16,
                    color);
            }
        }
    }

    private void drawWorldMapChunkHighlights(final DrawFeature feature,
                                             final long[] regionsArray,
                                             final int flooredCameraX,
                                             final int flooredCameraZ,
                                             final MatrixStack matrixStack,
                                             final VertexConsumer overlayBuffer
    ) {
        int color = feature.chunkHighlightProvider().colorSupplier().get();
        float a = ((color >> 24) & 255) / 255.0f;
        if (a == 0.0f) return;
        float r = (float)(color >> 16 & 255) / 255.0F;
        float g = (float)(color >> 8 & 255) / 255.0F;
        float b = (float)(color & 255) / 255.0F;
        for (int reg = 0; reg < regionsArray.length; reg++) {
            var highlights = feature.getChunkHighlights(regionsArray[reg]);
            for (int i = 0; i < highlights.size(); i++) {
                var chunkX = ChunkUtils.longToChunkX(highlights.getLong(i));
                var chunkZ = ChunkUtils.longToChunkZ(highlights.getLong(i));
                final float left = (float) (ChunkUtils.chunkCoordToCoord(chunkX) - flooredCameraX);
                final float top = (float) (ChunkUtils.chunkCoordToCoord(chunkZ) - flooredCameraZ);
                final float right = left + 16;
                final float bottom = top + 16;
                MinimapBackgroundDrawHelper.fillIntoExistingBuffer(
                    matrixStack.peek().getPositionMatrix(),
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
