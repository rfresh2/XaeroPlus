package xaeroplus.feature.render.highlights;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.LongList;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public interface ChunkHighlightCache {
    boolean addHighlight(final int x, final int z);
    boolean removeHighlight(final int x, final int z);
    boolean isHighlighted(final int x, final int z, ResourceKey<Level> dimensionId);
    LongList getHighlightsSnapshot(ResourceKey<Level> dimensionId);
    void handleWorldChange();
    void handleTick();
    void onEnable();
    void onDisable();
    Long2LongMap getHighlightsState();
    void loadPreviousState(Long2LongMap state);
}
