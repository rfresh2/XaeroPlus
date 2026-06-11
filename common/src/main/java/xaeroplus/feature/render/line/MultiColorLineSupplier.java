package xaeroplus.feature.render.line;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

@FunctionalInterface
public interface MultiColorLineSupplier {
    /**
     * @return Map of line to int color
     */
    Object2IntMap<Line> getLines(final int windowRegionX, final int windowRegionZ, final int windowRegionSize, final ResourceKey<Level> dimension);
}
