package xaeroplus.feature.render;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

@FunctionalInterface
public interface AsyncChunkHighlightSupplier {
    Long2LongMap getHighlights(final int windowRegionX, final int windowRegionZ, final int windowRegionSize, final ResourceKey<Level> dimension);
}
