package xaeroplus.feature.render;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

@FunctionalInterface
public interface DirectChunkHighlightSupplier {
    Long2LongMap getHighlights(final ResourceKey<Level> dimension);
}
