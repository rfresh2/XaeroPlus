package xaeroplus.event;

import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.common.eventhandler.Event;

public class ChunkDataEvent extends Event {
    private final boolean isFullChunk;
    private final Chunk chunk;

    public ChunkDataEvent(final boolean isFullChunk, final Chunk chunk) {
        this.isFullChunk = isFullChunk;
        this.chunk = chunk;
    }

    public boolean isFullChunk() {
        return this.isFullChunk;
    }

    public Chunk getChunk() {
        return this.chunk;
    }
}
