package wdl;

import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;

import java.util.List;
import java.util.Set;

/**
 * Shadow WDL class.
 * See https://github.com/Pokechu22/WorldDownloader/blob/v4/share/src/main/java/wdl/WDL.java
 */
public class WDL {
    public static boolean downloading;

    public static WDL getInstance() {
        return null;
    }

    // methods to get what chunks are saved
    // WDL doesn't actually save chunks until they are unloaded by the player
    // unless the download is stopped
    // so we'll want to render highlights over both of these

    // chunks unloaded and saved
    public Set<ChunkPos> savedChunks;
    // currently loaded chunks that will be saved
    public List<Chunk> getChunkList() {
        return null;
    }
}
