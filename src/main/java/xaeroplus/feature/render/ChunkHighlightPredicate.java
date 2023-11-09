package xaeroplus.feature.render;

import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

@FunctionalInterface
public interface ChunkHighlightPredicate {
    boolean isHighlighted(int chunkX, int chunkZ, RegistryKey<World> dimension);
}
