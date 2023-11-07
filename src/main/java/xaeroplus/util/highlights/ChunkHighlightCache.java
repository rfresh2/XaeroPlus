package xaeroplus.util.highlights;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

import java.util.List;

public interface ChunkHighlightCache {
    boolean addHighlight(final int x, final int z);
    boolean removeHighlight(final int x, final int z);
    boolean isHighlighted(final int x, final int z, RegistryKey<World> dimensionId);
    List<HighlightAtChunkPos> getHighlightsInRegion(final int leafRegionX, final int leafRegionZ, final int level, RegistryKey<World> dimension);
    void handleWorldChange();
    void handleTick();
    void onEnable();
    void onDisable();
    Long2LongMap getHighlightsState();
    void loadPreviousState(Long2LongMap state);
}
