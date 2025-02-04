package xaeroplus.feature.extensions;

import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import xaero.map.file.RegionDetection;
import xaero.map.highlight.RegionHighlightExistenceTracker;
import xaero.map.region.MapLayer;
import xaero.map.util.linked.LinkedChain;
import xaero.map.world.MapDimension;
import xaeroplus.XaeroPlus;
import xaeroplus.util.ChunkUtils;
import xaeroplus.util.DelegatingHashTable;

import java.util.Hashtable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.StampedLock;

public class OptimizedMapLayer extends MapLayer {
    private final MapDimension mapDimension;
    // todo: can we pack the region position into a single int?
    private final Long2ObjectOpenHashMap<RegionDetection> detectedRegions0;
    private final ReadWriteLock detectedRegionsLock = new StampedLock().asReadWriteLock();
    private final Long2ObjectOpenHashMap<RegionDetection> completeDetectedRegions0;
    private final ReadWriteLock completeDetectedRegionsLock = new StampedLock().asReadWriteLock();
    private final LinkedChain<RegionDetection> completeDetectedRegionsLinked;
    private int caveStart;

    public OptimizedMapLayer(MapDimension mapDimension, RegionHighlightExistenceTracker regionHighlightExistenceTracker) {
        super(mapDimension, regionHighlightExistenceTracker);
        this.mapDimension = mapDimension;
        this.detectedRegions0 = new Long2ObjectOpenHashMap<>();
        this.completeDetectedRegions0 = new Long2ObjectOpenHashMap<>();
        this.completeDetectedRegionsLinked = new LinkedChain<>();
    }

    @Override
    public void addRegionDetection(RegionDetection regionDetection) {
        try {
            if (detectedRegionsLock.writeLock().tryLock(1, TimeUnit.SECONDS)) {
                long packedPos = ChunkUtils.chunkPosToLong(regionDetection.getRegionX(), regionDetection.getRegionZ());
                detectedRegions0.put(packedPos, regionDetection);
                detectedRegionsLock.writeLock().unlock();
                tryAddingToCompleteRegionDetection(regionDetection);
            }
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Failed to acquire write lock for detectedRegions", e);
        }
    }

    @Override
    public RegionDetection getCompleteRegionDetection(int x, int z) {
        if (this.mapDimension.isUsingWorldSave()) {
            return this.mapDimension.getWorldSaveRegionDetection(x, z);
        } else {
            try {
                if (completeDetectedRegionsLock.readLock().tryLock(1, TimeUnit.SECONDS)) {
                    long packedPos = ChunkUtils.chunkPosToLong(x, z);
                    var rd = completeDetectedRegions0.get(packedPos);
                    completeDetectedRegionsLock.readLock().unlock();
                    return rd;
                }
            } catch (final Exception e) {
                XaeroPlus.LOGGER.error("Failed to acquire read lock for completeDetectedRegions", e);
            }
            return null;
        }
    }

    private boolean completeRegionDetectionContains(RegionDetection regionDetection) {
        return this.getCompleteRegionDetection(regionDetection.getRegionX(), regionDetection.getRegionZ()) != null;
    }

    @Override
    public void tryAddingToCompleteRegionDetection(RegionDetection regionDetection) {
        if (!completeRegionDetectionContains(regionDetection)) {
            if (this.mapDimension.isUsingWorldSave()) {
                this.mapDimension.addWorldSaveRegionDetection(regionDetection);
            } else {
                try {
                    if (completeDetectedRegionsLock.writeLock().tryLock(1, TimeUnit.SECONDS)) {
                        long packedPos = ChunkUtils.chunkPosToLong(regionDetection.getRegionX(), regionDetection.getRegionZ());
                        completeDetectedRegions0.put(packedPos, regionDetection);
                        completeDetectedRegionsLinked.add(regionDetection);
                        completeDetectedRegionsLock.writeLock().unlock();
                    }
                } catch (final Exception e) {
                    XaeroPlus.LOGGER.error("Failed to acquire write lock for completeDetectedRegions", e);
                }
            }
        }
    }

    @Override
    public RegionDetection getRegionDetection(int x, int z) {
        RegionDetection result = null;
        try {
            if (detectedRegionsLock.readLock().tryLock(1, TimeUnit.SECONDS)) {
                long packedPos = ChunkUtils.chunkPosToLong(x, z);
                result = detectedRegions0.get(packedPos);
                detectedRegionsLock.readLock().unlock();
            }
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Failed to acquire read lock for detectedRegions", e);
        }
        if (result == null) {
            RegionDetection worldSaveDetection = this.mapDimension.getWorldSaveRegionDetection(x, z);
            if (worldSaveDetection != null) {
                result = new RegionDetection(
                    worldSaveDetection.getWorldId(),
                    worldSaveDetection.getDimId(),
                    worldSaveDetection.getMwId(),
                    worldSaveDetection.getRegionX(),
                    worldSaveDetection.getRegionZ(),
                    worldSaveDetection.getRegionFile(),
                    worldSaveDetection.getInitialVersion(),
                    worldSaveDetection.isHasHadTerrain()
                );
                this.addRegionDetection(result);
                return result;
            }
        } else if (result.isRemoved()) {
            return null;
        }

        return result;
    }

    @Override
    public void removeRegionDetection(int x, int z) {
        if (this.mapDimension.getWorldSaveRegionDetection(x, z) != null) {
            RegionDetection regionDetection = this.getRegionDetection(x, z);
            if (regionDetection != null) {
                regionDetection.setRemoved(true);
            }
        } else {
            try {
                if (detectedRegionsLock.writeLock().tryLock(1, TimeUnit.SECONDS)) {
                    long packedPos = ChunkUtils.chunkPosToLong(x, z);
                    detectedRegions0.remove(packedPos);
                    detectedRegionsLock.writeLock().unlock();
                }
            } catch (final Exception e) {
                XaeroPlus.LOGGER.error("Failed to acquire write lock for detectedRegions", e);
            }
        }
    }

    @Override
    public Hashtable<Integer, Hashtable<Integer, RegionDetection>> getDetectedRegions() {
        DelegatingHashTable<Integer, Hashtable<Integer, RegionDetection>> delegateTable = new DelegatingHashTable<>();
        try {
            if (detectedRegionsLock.readLock().tryLock(1, TimeUnit.SECONDS)) {
                for (var entry : Long2ObjectMaps.fastIterable(detectedRegions0)) {
                    long packedPos = entry.getLongKey();
                    int x = ChunkUtils.longToChunkX(packedPos);
                    int z = ChunkUtils.longToChunkZ(packedPos);
                    RegionDetection regionDetection = entry.getValue();
                    Hashtable<Integer, RegionDetection> column = delegateTable.get(x);
                    if (column == null) {
                        column = new Hashtable<>();
                        delegateTable.put(x, column);
                    }
                    column.put(z, regionDetection);
                }
                detectedRegionsLock.readLock().unlock();
            }
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Failed to acquire read lock for detectedRegions", e);
        }
        return delegateTable;
    }

    @Override
    public Iterable<RegionDetection> getLinkedCompleteWorldSaveDetectedRegions() {
        return this.mapDimension.isUsingWorldSave()
            ? this.mapDimension.getLinkedWorldSaveDetectedRegions()
            : this.completeDetectedRegionsLinked;
    }

    @Override
    public void preDetection() {
        this.detectedRegions0.clear();
        this.completeDetectedRegions0.clear();
        this.completeDetectedRegionsLinked.reset();
    }

    public int getCaveStart() {
        return this.caveStart;
    }

    public void setCaveStart(int caveStart) {
        this.caveStart = caveStart;
    }
}
