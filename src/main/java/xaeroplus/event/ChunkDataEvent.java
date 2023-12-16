package xaeroplus.event;

import net.minecraft.world.chunk.WorldChunk;

public record ChunkDataEvent(WorldChunk chunk) {
}
