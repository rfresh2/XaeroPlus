package xaeroplus.util;

import net.minecraft.util.math.ChunkPos;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Function;

public class ChunkUtils {

    /** Caching helpers **/
    public static long chunkPosToLong(final ChunkPos chunkPos) {
        return (long)chunkPos.x & 4294967295L | ((long)chunkPos.z & 4294967295L) << 32;
    }

    public static long chunkPosToLong(final int x, final int z) {
        return (long)x & 4294967295L | ((long)z & 4294967295L) << 32;
    }

    public static ChunkPos longToChunkPos(final long l) {
        return new ChunkPos((int)(l & 4294967295L), (int)(l >> 32 & 4294967295L));
    }

    public static Callable<List<HighlightAtChunkPos>> loadHighlightChunksAtRegion(
            final int leafRegionX, final int leafRegionZ, final int level,
            final Function<Long, Boolean> highlightChunkPosFunction) {
        return () -> {
            final List<HighlightAtChunkPos> chunks = new ArrayList<>();
            final int mx = leafRegionX + level;
            final int mz = leafRegionZ + level;
            for (int regX = leafRegionX; regX < mx; ++regX) {
                for (int regZ = leafRegionZ; regZ < mz; ++regZ) {
                    for (int cx = 0; cx < 8; cx++) {
                        for (int cz = 0; cz < 8; cz++) {
                            final int mapTileChunkX = (regX << 3) + cx;
                            final int mapTileChunkZ = (regZ << 3) + cz;
                            for (int t = 0; t < 16; ++t) {
                                final int chunkPosX = (mapTileChunkX << 2) + t % 4;
                                final int chunkPosZ = (mapTileChunkZ << 2) + (t >> 2);
                                if (highlightChunkPosFunction.apply(ChunkUtils.chunkPosToLong(chunkPosX, chunkPosZ))) {
                                    chunks.add(new HighlightAtChunkPos(chunkPosX, chunkPosZ));
                                }
                            }
                        }
                    }
                }
            }
            return chunks;
        };
    }
}
