package xaeroplus.util;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import xaero.common.minimap.render.MinimapRendererHelper;
import xaero.map.region.MapTileChunk;
import xaeroplus.util.highlights.HighlightAtChunkPos;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class DrawManager {

    private final Map<Class<?>, ChunkHighlightDrawFeature> chunkHighlightDrawFeatures = new IdentityHashMap<>();

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
    ) {}

    public void registerChunkHighlightDrawFeature(Class<?> clazz, ChunkHighlightDrawFeature feature) {
        chunkHighlightDrawFeatures.put(clazz, feature);
    }

    public void drawMinimapFeatures(
        final MapTileChunk tileChunk,
        final int drawX,
        final int drawZ,
        final MatrixStack matrixStack,
        final VertexConsumer overlayBufferBuilder,
        MinimapRendererHelper helper
        ) {
        for (int t = 0; t < 16; ++t) {
            final int chunkPosX = tileChunk.getX() * 4 + t % 4;
            final int chunkPosZ = tileChunk.getZ() * 4 + t / 4;
            final float left = drawX + 16 * (t % 4);
            final float top = drawZ + 16 * (t / 4);
            for (ChunkHighlightDrawFeature value : chunkHighlightDrawFeatures.values()) {
                if (!value.enabled.get()) continue;
                int color = value.colorSupplier.get();
                float a = ((color >> 24) & 255) / 255.0f;
                if (a == 0.0f) continue;
                if (!value.minimapDrawPredicate.isHighlighted(chunkPosX, chunkPosZ, Shared.customDimensionId)) continue;
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

    public void drawWorldMapFeatures(
        final int leafRegionMinx,
        final int leafRegionMinZ,
        final int leveledSideInRegions,
        final int flooredCameraX,
        final int flooredCameraZ,
        final MatrixStack matrixStack,
        final VertexConsumer overlayBuffer
    ) {
        for (ChunkHighlightDrawFeature value : chunkHighlightDrawFeatures.values()) {
            if (!value.enabled.get()) continue;
            int color = value.colorSupplier.get();
            float a = ((color >> 24) & 255) / 255.0f;
            if (a == 0.0f) continue;
            var highlights = value.worldMapDrawPredicate.highlightsInRegion(leafRegionMinx, leafRegionMinZ, leveledSideInRegions, Shared.customDimensionId);
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
