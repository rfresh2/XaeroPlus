package xaeroplus.feature.extensions;

import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import xaero.map.region.LeveledRegion;
import xaero.map.region.MapRegion;

public interface CustomDimensionMapProcessor {
    boolean regionExistsCustomDimension(int caveLayer, int x, int z, RegistryKey<World> dimId);
    boolean regionExistsCustomDimension(int x, int z, RegistryKey<World> dimId);
    boolean regionDetectionExistsCustomDimension(int caveLayer, int x, int z, RegistryKey<World> dimId);
    LeveledRegion<?> getLeveledRegionCustomDimension(int caveLayer, int leveledRegX, int leveledRegZ, int level, RegistryKey<World> dimId);
    MapRegion getMapRegionCustomDimension(int caveLayer, int regX, int regZ, boolean create, RegistryKey<World> dimId);
    MapRegion getMapRegionCustomDimension(int regX, int regZ, boolean create, RegistryKey<World> dimId);
}
