package xaeroplus.util.newchunks;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import xaeroplus.util.HighlightAtChunkPos;

import java.util.List;

public interface NewChunksCache {
    void addNewChunk(final int x, final int z);
    boolean isNewChunk(final int chunkPosX, final int chunkPosZ, final int dimensionId);
    List<HighlightAtChunkPos> getNewChunksInRegion(final int leafRegionX, final int leafRegionZ, final int level, int dimension);
    void handleWorldChange();
    void handleTick();
    void onEnable();
    void onDisable();
    Long2LongOpenHashMap getNewChunksState();
    void loadPreviousState(Long2LongOpenHashMap state);
}
