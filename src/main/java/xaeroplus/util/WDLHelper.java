package xaeroplus.util;

import com.google.common.base.Suppliers;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import wdl.WDL;
import xaeroplus.XaeroPlus;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class WDLHelper {
    private static int wdlColor = XaeroPlus.getColor(0, 255, 0, 100);
    // getting the set of saved chunks is expensive. This supplier acts as a cache to speed things up.
    private static final Supplier<Set<ChunkPos>> getChunkSupplier = Suppliers.memoizeWithExpiration(WDLHelper::getSavedChunks, 500, TimeUnit.MILLISECONDS);
    private static boolean hasLoggedFail = false;
    private static boolean checkedWdlPresent = false;
    private static boolean isWdlPresent = false;

    public static boolean isWdlPresent() {
        if (!checkedWdlPresent) {
            checkedWdlPresent = true;
            try {
                Class.forName(WDL.class.getName());
                isWdlPresent = true;
            } catch (final Throwable e) {
                if (!hasLoggedFail) {
                    System.out.println("[XaeroPlus] WDL mod not present, WDL features will be disabled.");
                    hasLoggedFail = true;
                }
                isWdlPresent = false;
            }
        }
        return isWdlPresent;
    }

    public static boolean isDownloading() {
        try {
            return WDL.downloading;
        } catch (final Throwable e) {
            return false;
        }
    }

    public static Set<ChunkPos> getSavedChunksWithCache() {
        return getChunkSupplier.get();
    }

    private static Set<ChunkPos> getSavedChunks() {
        try {
            final List<ChunkPos> loadedChunks = WDL.getInstance().getChunkList().stream().map(Chunk::getPos).collect(Collectors.toList());
            final Set<ChunkPos> savedChunks = new HashSet<>(WDL.getInstance().savedChunks);
            savedChunks.addAll(loadedChunks);
            return savedChunks;
        } catch (final Throwable e) {
            if (!hasLoggedFail) {
                System.out.println("[XaeroPlus] Error: Failed getting WDL chunks");
                e.printStackTrace();
                hasLoggedFail = true;
            }
        }
        return Collections.emptySet();
    }

    public static int getWdlColor() {
        return wdlColor;
    }

    public static void setAlpha(float a) {
        wdlColor = XaeroPlus.getColor(0, 255, 0, (int) a);
    }
}
