package xaeroplus.module.impl;

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
    public ResourceKey<Level> dimension() {
        return Level.OVERWORLD;
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
        int spawnBlockX = levelData.getSpawnPos().getX();
        int spawnBlockZ = levelData.getSpawnPos().getZ();
        int spawnChunkX = ChunkUtils.posToChunkPos(spawnBlockX);
        int spawnChunkZ = ChunkUtils.posToChunkPos(spawnBlockZ);
        return ChunkUtils.chunkPosToLong(spawnChunkX, spawnChunkZ);
    }
}
