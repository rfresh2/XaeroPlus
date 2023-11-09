package xaeroplus.feature.extensions;

import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

public interface CustomDimensionMapSaveLoad {
    void detectRegionsInDimension(final int attempts, final RegistryKey<World> dimId);
}
