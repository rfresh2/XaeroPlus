package xaeroplus.util;

import static xaeroplus.util.ChunkUtils.longToChunkX;
import static xaeroplus.util.ChunkUtils.longToChunkZ;

public class NewChunkData {
    public final int x;
    public final int z;
    public final long foundTime;

    public NewChunkData(final long chunkPos, final long foundTime) {
        this.x = longToChunkX(chunkPos);
        this.z = longToChunkZ(chunkPos);
        this.foundTime = foundTime;
    }

    public NewChunkData(final int x, final int z, final int foundTime) {
        this.x = x;
        this.z = z;
        this.foundTime = (long) foundTime;
    }
}
