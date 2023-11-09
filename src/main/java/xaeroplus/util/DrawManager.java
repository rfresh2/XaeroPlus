package xaeroplus.util;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongList;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import xaero.common.minimap.render.MinimapRendererHelper;
import xaeroplus.util.highlights.HighlightAtChunkPos;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class DrawManager {

    private final Map<Class<?>, DrawFeature> chunkHighlightDrawFeatures = new IdentityHashMap<>();

    // todo: refactor and unify minimap and worldmap draw interfaces
    //  we want to utilize caching and caffeine async cache loading as much as possible
    //  worldmap drawing does this to some extent, but minimap drawing does not
    //  ideally we draw all highlights in one op per frame
    //  with inputs being the coords of the map view rect (or some extended rect that changes infrequently while panning small distances)
    //  fewer change to inputs is good -> fewer cache misses
    //  fewer cache lookup ops is good -> fewer ops per frame
    //  this is also the only way we can scale the minimap further without impacting fps horribly

    // todo: consider how to centralize the caching of highlight data here
    //  we currently have region render caches in each module for their highlights
    //  it'd be better for this manager to own the render caching so we can modify it in one place
    //  and so we can have a single cache for all highlights, reducing cache misses and reducing cache loading costs


    @FunctionalInterface
    public interface MinimapChunkHighlightDrawPredicate {
        boolean isHighlighted(int chunkX, int chunkZ, RegistryKey<World> dimension);
    }

    @FunctionalInterface
    public interface WorldMapChunkHighlightDrawPredicate {
        List<HighlightAtChunkPos> highlightsInRegion(int leafRegionMinx, int leafRegionMinZ, int leveledSideInRegions, RegistryKey<World> dimension);
    }

    public record ChunkHighlightDrawFeature(
        Supplier<Boolean> enabled,
        MinimapChunkHighlightDrawPredicate minimapDrawPredicate,
        WorldMapChunkHighlightDrawPredicate worldMapDrawPredicate,
        Supplier<Integer> colorSupplier
    ) {
    }

    record DrawFeature(
        ChunkHighlightDrawFeature chunkHighlightDrawFeature,
        AsyncLoadingCache<Long, LongList> chunkRenderCache
    ) {
        public LongList getChunkHighlights(final long regionLong) {
            return chunkRenderCache.get(regionLong).getNow(LongList.of());
        }
    }

    public void registerChunkHighlightDrawFeature(Class<?> clazz, ChunkHighlightDrawFeature feature) {
        chunkHighlightDrawFeatures.put(clazz, new DrawFeature(feature, Caffeine.newBuilder()
            .expireAfterWrite(3000, java.util.concurrent.TimeUnit.MILLISECONDS)
            .refreshAfterWrite(500, java.util.concurrent.TimeUnit.MILLISECONDS)
            .executor(Shared.cacheRefreshExecutorService.get())
            .buildAsync(regionLong -> {
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
                            if (feature.minimapDrawPredicate.isHighlighted(chunkPosX, chunkPosZ, Shared.customDimensionId)) {
                                list.add(ChunkUtils.chunkPosToLong(chunkPosX, chunkPosZ));
                            }
                        }
                    }
                }
                return list;
            })));
    }

    public void drawMinimapFeatures(
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
        for (DrawFeature value : chunkHighlightDrawFeatures.values()) {
            if (!value.chunkHighlightDrawFeature.enabled.get()) continue;
            int color = value.chunkHighlightDrawFeature.colorSupplier.get();
            float a = ((color >> 24) & 255) / 255.0f;
            if (a == 0.0f) continue;
            for (int r = 0; r < regionsArray.length; r++) {
                var regionLong = regionsArray[r];
                var regionX = ChunkUtils.longToChunkX(regionLong);
                var regionZ = ChunkUtils.longToChunkZ(regionLong);
                var cx = regionX & 7;
                var cz = regionZ & 7;
                var drawX = ((cx - chunkX) << 6) - (tileX << 4) - insideX;
                var drawZ = ((cz - chunkZ) << 6) - (tileZ << 4) - insideZ;
                final LongList highlights = value.getChunkHighlights(regionLong);
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
    }

    public void drawWorldMapFeatures(
        final int leafRegionMinx,
        final int leafRegionMinZ,
        final int leveledSideInRegions,
        final int flooredCameraX,
        final int flooredCameraZ,
        final MatrixStack matrixStack,
        final VertexConsumer overlayBuffer
    ) {
        for (DrawFeature value : chunkHighlightDrawFeatures.values()) {
            if (!value.chunkHighlightDrawFeature.enabled.get()) continue;
            int color = value.chunkHighlightDrawFeature.colorSupplier.get();
            float a = ((color >> 24) & 255) / 255.0f;
            if (a == 0.0f) continue;
            var highlights = value.chunkHighlightDrawFeature.worldMapDrawPredicate.highlightsInRegion(leafRegionMinx, leafRegionMinZ, leveledSideInRegions, Shared.customDimensionId);
            if (highlights.isEmpty()) continue;
            float r = (float)(color >> 16 & 255) / 255.0F;
            float g = (float)(color >> 8 & 255) / 255.0F;
            float b = (float)(color & 255) / 255.0F;
            for (int i = 0; i < highlights.size(); i++) {
                HighlightAtChunkPos c = highlights.get(i);
                final float left = (float) ((c.x() << 4) - flooredCameraX);
                final float top = (float) ((c.z() << 4) - flooredCameraZ);
                final float right = left + 16;
                final float bottom = top + 16;
                GuiHelper.fillIntoExistingBuffer(
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
