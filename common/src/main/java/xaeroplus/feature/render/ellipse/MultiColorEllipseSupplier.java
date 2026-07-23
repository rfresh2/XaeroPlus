package xaeroplus.feature.render.ellipse;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

@FunctionalInterface
public interface MultiColorEllipseSupplier {
    /**
     * @return Map of ellipse to int color
     */
    Object2IntMap<Ellipse> getEllipses(final int windowRegionX, final int windowRegionZ, final int windowRegionSize, final ResourceKey<Level> dimension);
}
