package xaeroplus.event;

import com.collarmc.pounce.EventInfo;
import net.minecraft.world.chunk.WorldChunk;

@EventInfo()
public record ChunkDataEvent(WorldChunk chunk) {
}
