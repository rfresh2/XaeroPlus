package xaeroplus.feature.render;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

@FunctionalInterface
public interface ChunkHighlightPredicate {
    boolean isHighlighted(int chunkX, int chunkZ, ResourceKey<Level> dimension);
}
