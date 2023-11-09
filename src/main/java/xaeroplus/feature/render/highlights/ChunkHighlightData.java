package xaeroplus.feature.render.highlights;

import static xaeroplus.util.ChunkUtils.longToChunkX;
import static xaeroplus.util.ChunkUtils.longToChunkZ;

public record ChunkHighlightData(int x, int z, long foundTime) {
    public ChunkHighlightData(final long chunkPos, final long foundTime) {
        this(longToChunkX(chunkPos), longToChunkZ(chunkPos), foundTime);
    }
}
