package xaeroplus.feature.render.highlights;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaeroplus.event.XaeroWorldChangeEvent;

public interface ChunkHighlightCache {
    void addHighlight(final int x, final int z);
    void removeHighlight(final int x, final int z);
    boolean isHighlighted(final int x, final int z, ResourceKey<Level> dimensionId);
    Long2LongMap getHighlightsState(ResourceKey<Level> dimensionId);
    void handleWorldChange(final XaeroWorldChangeEvent event);
    void handleTick();
    void onEnable();
    void onDisable();
}
