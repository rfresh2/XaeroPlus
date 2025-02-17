package xaeroplus.module.impl;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongMaps;
import net.lenni0451.lambdaevents.EventHandler;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaeroplus.event.ClientTickEvent;
import xaeroplus.util.ChunkUtils;

public class SpawnChunksPlayer extends SpawnChunksBase {

    @EventHandler
    public void onClientTick(ClientTickEvent.Post event) {
        onClientTick();
    }

    @Override
    public Long2LongMap entityProcessing(ResourceKey<Level> dimension) {
        if (dimension != ChunkUtils.getActualDimension()) return Long2LongMaps.EMPTY_MAP;
        return entityProcessingCache;
    }

    @Override
    public Long2LongMap redstoneProcessing(ResourceKey<Level> dimension) {
        if (dimension != ChunkUtils.getActualDimension()) return Long2LongMaps.EMPTY_MAP;
        return redstoneProcessingCache;
    }

    @Override
    public Long2LongMap lazyChunks(ResourceKey<Level> dimension) {
        if (dimension != ChunkUtils.getActualDimension()) return Long2LongMaps.EMPTY_MAP;
        return lazyChunksCache;
    }

    @Override
    public Long2LongMap outerChunks(ResourceKey<Level> dimension) {
        if (dimension != ChunkUtils.getActualDimension()) return Long2LongMaps.EMPTY_MAP;
        return outerChunksCache;
    }

    @Override
    int getSpawnRadius() {
        var level = mc.level;
        if (level == null) return 0;
        return level.getServerSimulationDistance();
    }

    @Override
    long getSpawnChunkPos() {
        return ChunkUtils.chunkPosToLong(ChunkUtils.actualPlayerChunkX(), ChunkUtils.actualPlayerChunkZ());
    }
}
