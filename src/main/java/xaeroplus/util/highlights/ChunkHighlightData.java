package xaeroplus.util.highlights;

import static xaeroplus.util.ChunkUtils.longToChunkX;
import static xaeroplus.util.ChunkUtils.longToChunkZ;

public class ChunkHighlightData {
    public final int x;
    public final int z;
    public final long foundTime;

    public ChunkHighlightData(final long chunkPos, final long foundTime) {
        this.x = longToChunkX(chunkPos);
        this.z = longToChunkZ(chunkPos);
        this.foundTime = foundTime;
    }

    public ChunkHighlightData(final int x, final int z, final int foundTime) {
        this.x = x;
        this.z = z;
        this.foundTime = foundTime;
    }

    public ChunkHighlightData(final int x, final int z, final long foundTime) {
        this.x = x;
        this.z = z;
        this.foundTime = foundTime;
    }
}
