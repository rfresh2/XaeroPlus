package xaeroplus.feature.extensions;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import xaero.map.region.BranchLeveledRegion;
import xaero.map.region.LeveledRegion;
import xaero.map.region.LeveledRegionManager;
import xaero.map.region.MapRegion;
import xaeroplus.Globals;
import xaeroplus.XaeroPlus;
import xaeroplus.mixin.client.AccessorBranchLeveledRegion;
import xaeroplus.mixin.client.AccessorMapRegion;
import xaeroplus.util.ChunkUtils;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.StampedLock;

public class OptimizedLeveledRegionManager extends LeveledRegionManager {
    private final Long2ObjectOpenHashMap<LeveledRegion<?>> regionTextureMap0 = new Long2ObjectOpenHashMap<>();
    private final ReadWriteLock mapLock = new StampedLock().asReadWriteLock();

    @Override
    public void putLeaf(int X, int Z, MapRegion leaf) {
        int maxLevelX = X >> Globals.MAX_REGION_LEVEL;
        int maxLevelZ = Z >> Globals.MAX_REGION_LEVEL;
        long packed = ChunkUtils.chunkPosToLong(maxLevelX, maxLevelZ);
        try {
            if (mapLock.writeLock().tryLock(1, TimeUnit.SECONDS)) {
                LeveledRegion<?> rootBranch = regionTextureMap0.get(packed);
                if (rootBranch == null) {
                    rootBranch = new BranchLeveledRegion(
                        leaf.getWorldId(), leaf.getDimId(), leaf.getMwId(), leaf.getDim(), Globals.MAX_REGION_LEVEL, maxLevelX, maxLevelZ,
                        leaf.getCaveLayer(), null
                    );
                    regionTextureMap0.put(packed, rootBranch);
                    leaf.getDim().getLayeredMapRegions().addListRegion(rootBranch);
                }

                if (!(rootBranch instanceof MapRegion)) {
                    if (rootBranch instanceof BranchLeveledRegion) {
                        ((AccessorBranchLeveledRegion) rootBranch).invokePutLeaf(X, Z, leaf);
                    } else {
                        XaeroPlus.LOGGER.error("Root branch: {} is not a BranchLeveledRegion!", rootBranch.getClass().getName());
                    }
                }
                mapLock.writeLock().unlock();
            }
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error while putting leaf: ", e);
        }
    }

    @Override
    public LeveledRegion<?> get(int leveledX, int leveledZ, int level) {
        if (level > Globals.MAX_REGION_LEVEL) {
            throw new RuntimeException(new IllegalArgumentException());
        } else {
            int maxLevelX = leveledX >> Globals.MAX_REGION_LEVEL - level;
            int maxLevelZ = leveledZ >> Globals.MAX_REGION_LEVEL - level;
            long packed = ChunkUtils.chunkPosToLong(maxLevelX, maxLevelZ);
            try {
                if (mapLock.readLock().tryLock(1, TimeUnit.SECONDS)) {
                    LeveledRegion<?> rootBranch = regionTextureMap0.get(packed);
                    if (rootBranch == null) {
                        mapLock.readLock().unlock();
                        return null;
                    }
                    LeveledRegion<?> result = null;
                    if (level == Globals.MAX_REGION_LEVEL) {
                        result = rootBranch;
                    } else {
                        if (rootBranch instanceof MapRegion) {
                            result = ((AccessorMapRegion) rootBranch).invokeGet(leveledX, leveledZ, level);
                        } else if (rootBranch instanceof BranchLeveledRegion) {
                            result = ((AccessorBranchLeveledRegion) rootBranch).invokeGet(leveledX, leveledZ, level);
                        } else {
                            XaeroPlus.LOGGER.error("Root branch: {} is not a known type!", rootBranch.getClass().getName());
                        }
                    }
                    mapLock.readLock().unlock();
                    return result;
                }
            } catch (final Exception e) {
                XaeroPlus.LOGGER.error("Error while getting region: ", e);
            }
        }
        return null;
    }

    @Override
    public boolean remove(int leveledX, int leveledZ, int level) {
        if (level > Globals.MAX_REGION_LEVEL) {
            throw new RuntimeException(new IllegalArgumentException());
        } else {
            int maxLevelX = leveledX >> Globals.MAX_REGION_LEVEL - level;
            int maxLevelZ = leveledZ >> Globals.MAX_REGION_LEVEL - level;
            long packed = ChunkUtils.chunkPosToLong(maxLevelX, maxLevelZ);
            try {
                if (mapLock.writeLock().tryLock(1, TimeUnit.SECONDS)) {
                    LeveledRegion<?> rootBranch = regionTextureMap0.get(packed);
                    if (rootBranch == null) {
                        mapLock.writeLock().unlock();
                        return false;
                    }
                    if (!(rootBranch instanceof MapRegion)) {
                        if (rootBranch instanceof BranchLeveledRegion) {
                            mapLock.writeLock().unlock();
                            return ((AccessorBranchLeveledRegion) rootBranch).invokeRemove(leveledX, leveledZ, level);
                        } else {
                            XaeroPlus.LOGGER.error("Root branch: {} is not a BranchLeveledRegion!", rootBranch.getClass().getName());
                            mapLock.writeLock().unlock();
                            return false;
                        }
                    }
                    regionTextureMap0.remove(packed);
                    mapLock.writeLock().unlock();
                    return true;
                }
            } catch (final Exception e) {
                XaeroPlus.LOGGER.error("Error while removing region: ", e);
            }
        }
        return false;
    }
}
