package xaeroplus.module.impl;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongMaps;
import net.lenni0451.lambdaevents.EventHandler;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import xaeroplus.XaeroPlus;
import xaeroplus.event.ClientTickEvent;
import xaeroplus.util.ChunkUtils;

public class SpawnChunks extends SpawnChunksBase {

    @EventHandler
    public void onClientTick(ClientTickEvent.Post event) {
        onClientTick();
    }

    @Override
    public Long2LongMap entityProcessing(ResourceKey<Level> dimension) {
        if (dimension != Level.OVERWORLD) return Long2LongMaps.EMPTY_MAP;
        return entityProcessingCache;
    }

    @Override
    public Long2LongMap redstoneProcessing(ResourceKey<Level> dimension) {
        if (dimension != Level.OVERWORLD) return Long2LongMaps.EMPTY_MAP;
        return redstoneProcessingCache;
    }

    @Override
    public Long2LongMap lazyChunks(ResourceKey<Level> dimension) {
        if (dimension != Level.OVERWORLD) return Long2LongMaps.EMPTY_MAP;
        return lazyChunksCache;
    }

    @Override
    public Long2LongMap outerChunks(ResourceKey<Level> dimension) {
        if (dimension != Level.OVERWORLD) return Long2LongMaps.EMPTY_MAP;
        return outerChunksCache;
    }

    @Override
    int getSpawnRadius() {
        int spawnChunkRadius = 2;
        if (mc.hasSingleplayerServer()) {
            try {
                spawnChunkRadius = mc.getSingleplayerServer().getLevel(Level.OVERWORLD).getGameRules().getInt(GameRules.RULE_SPAWN_RADIUS);
            } catch (final Exception e) {
                XaeroPlus.LOGGER.error("Failed to get spawn radius from singleplayer server", e);
            }
        }
        return spawnChunkRadius;
    }

    @Override
    long getSpawnChunkPos() {
        var level = mc.level;
        if (level == null) return ChunkUtils.chunkPosToLong(0, 0);
        ClientLevel.ClientLevelData levelData = level.getLevelData();
        if (levelData == null) return ChunkUtils.chunkPosToLong(0, 0);
        int spawnBlockX = levelData.getXSpawn();
        int spawnBlockZ = levelData.getZSpawn();
        int spawnChunkX = ChunkUtils.posToChunkPos(spawnBlockX);
        int spawnChunkZ = ChunkUtils.posToChunkPos(spawnBlockZ);
        return ChunkUtils.chunkPosToLong(spawnChunkX, spawnChunkZ);
    }
}
