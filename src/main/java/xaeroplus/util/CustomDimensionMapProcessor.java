package xaeroplus.util;

import xaero.map.file.RegionDetection;
import xaero.map.region.LeveledRegion;
import xaero.map.region.MapRegion;

public interface CustomDimensionMapProcessor {
    boolean regionExistsCustomDimension(int x, int z, int dimId);
    boolean regionDetectionExistsCustomDimension(int x, int z, int dimId);
    RegionDetection getRegionDetectionCustomDimension(int x, int z, int dimId);
    void removeRegionDetectionCustomDimension(int x, int z, int dimId);
    LeveledRegion<?> getLeveledRegionCustomDimension(int leveledRegX, int leveledRegZ, int level, int dimId);
    MapRegion getMapRegionCustomDimension(int regX, int regZ, boolean create, int dimId);
}
