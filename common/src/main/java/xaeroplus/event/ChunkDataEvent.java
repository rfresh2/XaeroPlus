package xaeroplus.event;

import net.minecraft.world.level.chunk.LevelChunk;

public record ChunkDataEvent(LevelChunk chunk, boolean seenChunk) {
}
