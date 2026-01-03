package xaeroplus.feature.extensions;

import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import xaero.map.file.RegionDetection;
import xaero.map.highlight.RegionHighlightExistenceTracker;
import xaero.map.region.MapLayer;
import xaero.map.util.linked.LinkedChain;
import xaero.map.world.MapDimension;
import xaeroplus.util.ChunkUtils;

import java.util.Hashtable;

/**
 * at very low zooms, the worldmap spends an inordinate amount of time doing region detection lookups
 *
 * the original MapLayer class here stores these detections in a nested table: Integer [x] -> HashTable<Integer [z]>
 *
 * two issues:
 * 1. integer boxing, ints are constantly being converted to objects for the hashtable lookup
 *  (integer boxing is fine for lower numbers where the jvm optimizes them, but not large numbers)
 * 2. the nested hashtable-in-hashtable design is slow
 *
 */
public class OptimizedMapLayer extends MapLayer {
    private final MapDimension mapDimension;
    private final Long2ObjectOpenHashMap<RegionDetection> detectedRegions0;
    private final Long2ObjectOpenHashMap<RegionDetection> completeDetectedRegions0;
    private final LinkedChain<RegionDetection> completeDetectedRegionsLinked;

    public OptimizedMapLayer(MapDimension mapDimension, RegionHighlightExistenceTracker regionHighlightExistenceTracker) {
        super(mapDimension, regionHighlightExistenceTracker);
        this.mapDimension = mapDimension;
        this.detectedRegions0 = new Long2ObjectOpenHashMap<>();
        this.completeDetectedRegions0 = new Long2ObjectOpenHashMap<>();
        this.completeDetectedRegionsLinked = new LinkedChain<>();
    }

    @Override
    public void addRegionDetection(RegionDetection regionDetection) {
        synchronized (detectedRegions0) {
            long packedPos = ChunkUtils.chunkPosToLong(regionDetection.getRegionX(), regionDetection.getRegionZ());
            detectedRegions0.put(packedPos, regionDetection);
            tryAddingToCompleteRegionDetection(regionDetection);
        }
    }

    @Override
    public RegionDetection getCompleteRegionDetection(int x, int z) {
        if (this.mapDimension.isUsingWorldSave()) {
            return this.mapDimension.getWorldSaveRegionDetection(x, z);
        }

        synchronized (completeDetectedRegions0) {
            long packedPos = ChunkUtils.chunkPosToLong(x, z);
            return completeDetectedRegions0.get(packedPos);
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
                synchronized (completeDetectedRegions0) {
                    long packedPos = ChunkUtils.chunkPosToLong(regionDetection.getRegionX(), regionDetection.getRegionZ());
                    completeDetectedRegions0.put(packedPos, regionDetection);
                    completeDetectedRegionsLinked.add(regionDetection);
                }
            }
        }
    }

    @Override
    public RegionDetection getRegionDetection(int x, int z) {
        RegionDetection result = null;
        synchronized (detectedRegions0) {
            long packedPos = ChunkUtils.chunkPosToLong(x, z);
            result = detectedRegions0.get(packedPos);
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
            synchronized (detectedRegions0) {
                long packedPos = ChunkUtils.chunkPosToLong(x, z);
                detectedRegions0.remove(packedPos);
            }
        }
    }

    // have to do a full copy to convert to hashtable, but this is only used by the PNGExporter
    @Override
    public Hashtable<Integer, Hashtable<Integer, RegionDetection>> getDetectedRegions() {
        Hashtable<Integer, Hashtable<Integer, RegionDetection>> resultTable = new Hashtable<>();
        synchronized (detectedRegions0) {
            for (var entry : Long2ObjectMaps.fastIterable(detectedRegions0)) {
                long packedPos = entry.getLongKey();
                int x = ChunkUtils.longToChunkX(packedPos);
                int z = ChunkUtils.longToChunkZ(packedPos);
                RegionDetection regionDetection = entry.getValue();
                Hashtable<Integer, RegionDetection> column = resultTable.get(x);
                if (column == null) {
                    column = new Hashtable<>();
                    resultTable.put(x, column);
                }
                column.put(z, regionDetection);
            }
        }
        return resultTable;
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
}
