package xaeroplus.util.normalizer;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Deletes {@code .xwmc} cache directories after region conversion.
 *
 * <p>Cache data contains rendering-dependent texture buffers tightly coupled to the
 * runtime rendering pipeline. After converting region files between versions, caches
 * must be deleted and allowed to regenerate on next client load.</p>
 *
 * <p>Cache directory pattern: {@code cache_<globalVersion>/} containing {@code <x>_<z>.xwmc} files.</p>
 */
public final class CacheInvalidator {
    private CacheInvalidator() {}

    /**
     * Delete all cache directories under the given save folder.
     *
     * @param saveFolder root of the XaeroWorldMap save folder
     * @return list of paths that were deleted
     * @throws IOException if directory walking fails
     */
    public static List<Path> invalidateAll(Path saveFolder) throws IOException {
        List<Path> deleted = new ArrayList<>();
        if (!Files.isDirectory(saveFolder)) return deleted;

        Files.walk(saveFolder)
            .filter(Files::isDirectory)
            .filter(p -> p.getFileName().toString().startsWith("cache_"))
            .sorted((a, b) -> b.toString().length() - a.toString().length()) // deepest first
            .forEach(cacheDir -> {
                try {
                    deleteRecursive(cacheDir);
                    deleted.add(cacheDir);
                } catch (IOException e) {
                    System.err.println("[CacheInvalidator] Failed to delete: " + cacheDir + ": " + e.getMessage());
                }
            });

        return deleted;
    }

    /**
     * Delete all cache directories under a specific world/dimension path.
     *
     * @param dimDir the dimension directory (e.g. {@code saveFolder/worldId/dimId})
     * @return list of paths that were deleted
     * @throws IOException if directory walking fails
     */
    public static List<Path> invalidateDimension(Path dimDir) throws IOException {
        List<Path> deleted = new ArrayList<>();
        if (!Files.isDirectory(dimDir)) return deleted;

        try (DirectoryStream<Path> entries = Files.newDirectoryStream(dimDir, Files::isDirectory)) {
            for (Path entry : entries) {
                if (entry.getFileName().toString().startsWith("cache_")) {
                    deleteRecursive(entry);
                    deleted.add(entry);
                }
            }
        }

        return deleted;
    }

    private static void deleteRecursive(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        Files.walk(dir)
            .sorted((a, b) -> b.toString().length() - a.toString().length()) // files before dirs
            .forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    // best effort
                }
            });
    }
}
