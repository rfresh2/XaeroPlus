package xaeroplus.feature.render;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.List;

@FunctionalInterface
public interface LineSupplier {
    List<Line> getLines(final int windowRegionX, final int windowRegionZ, final int windowRegionSize, final ResourceKey<Level> dimension);
}
