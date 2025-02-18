package xaeroplus.module.impl;

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
    public ResourceKey<Level> dimension() {
        return ChunkUtils.getActualDimension();
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
