package xaeroplus.util.highlights;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

public interface ChunkHighlightCache {
    boolean addHighlight(final int x, final int z);
    boolean removeHighlight(final int x, final int z);
    boolean isHighlighted(final int x, final int z, RegistryKey<World> dimensionId);
    void handleWorldChange();
    void handleTick();
    void onEnable();
    void onDisable();
    Long2LongMap getHighlightsState();
    void loadPreviousState(Long2LongMap state);
}
