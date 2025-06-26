package xaeroplus.feature.render.text;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public interface TextSupplier {
    Long2ObjectMap<Text> getText(final int windowRegionX, final int windowRegionZ, final int windowRegionSize, final ResourceKey<Level> dimension);
}
