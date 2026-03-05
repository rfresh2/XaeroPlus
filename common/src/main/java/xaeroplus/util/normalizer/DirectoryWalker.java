package xaeroplus.util.normalizer;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Walks the Xaero's World Map save directory structure and enumerates all region files.
 *
 * <p>Directory layout:
 * <pre>
 * &lt;saveFolder&gt;/
 *   &lt;worldId&gt;/
 *     &lt;dimId&gt;/
 *       [&lt;mwId&gt;/]
 *         &lt;x&gt;_&lt;z&gt;.zip         (or .xaero for legacy)
 *         caves/
 *           &lt;layerInt&gt;/
 *             &lt;x&gt;_&lt;z&gt;.zip
 * </pre>
 */
public final class DirectoryWalker {
    private DirectoryWalker() {}

    /** Metadata about a discovered region file, extracted from its path. */
    public record RegionFileInfo(
        Path path,
        String worldId,
        String dimId,
        String mwId,
        int caveLayer,
        int regionX,
        int regionZ
    ) {}

    private static final Pattern REGION_FILE_PATTERN = Pattern.compile("^(-?\\d+)_(-?\\d+)\\.(zip|xaero)$");

    /**
     * Enumerate all region files under the given save folder.
     */
    public static List<RegionFileInfo> enumerate(Path saveFolder) throws IOException {
        if (!Files.isDirectory(saveFolder)) return List.of();

        List<RegionFileInfo> results = new ArrayList<>();

        try (DirectoryStream<Path> worlds = Files.newDirectoryStream(saveFolder, Files::isDirectory)) {
            for (Path worldDir : worlds) {
                String worldId = worldDir.getFileName().toString();
                try (DirectoryStream<Path> dims = Files.newDirectoryStream(worldDir, Files::isDirectory)) {
                    for (Path dimDir : dims) {
                        String dimId = dimDir.getFileName().toString();
                        scanDimDirectory(dimDir, worldId, dimId, null, results);
                    }
                }
            }
        }

        return results;
    }

    private static void scanDimDirectory(
        Path dimDir, String worldId, String dimId, String mwId, List<RegionFileInfo> results
    ) throws IOException {
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(dimDir)) {
            for (Path entry : entries) {
                String name = entry.getFileName().toString();

                if (Files.isRegularFile(entry)) {
                    Matcher m = REGION_FILE_PATTERN.matcher(name);
                    if (m.matches()) {
                        results.add(new RegionFileInfo(
                            entry, worldId, dimId, mwId, Integer.MAX_VALUE,
                            Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))
                        ));
                    }
                } else if (Files.isDirectory(entry)) {
                    if ("caves".equals(name)) {
                        scanCavesDirectory(entry, worldId, dimId, mwId, results);
                    } else if (!name.startsWith("cache_") && mwId == null) {
                        scanDimDirectory(entry, worldId, dimId, name, results);
                    }
                }
            }
        }
    }

    private static void scanCavesDirectory(
        Path cavesDir, String worldId, String dimId, String mwId, List<RegionFileInfo> results
    ) throws IOException {
        try (DirectoryStream<Path> layers = Files.newDirectoryStream(cavesDir, Files::isDirectory)) {
            for (Path layerDir : layers) {
                int caveLayer;
                try {
                    caveLayer = Integer.parseInt(layerDir.getFileName().toString());
                } catch (NumberFormatException e) {
                    continue;
                }

                try (DirectoryStream<Path> files = Files.newDirectoryStream(layerDir, Files::isRegularFile)) {
                    for (Path file : files) {
                        Matcher m = REGION_FILE_PATTERN.matcher(file.getFileName().toString());
                        if (m.matches()) {
                            results.add(new RegionFileInfo(
                                file, worldId, dimId, mwId, caveLayer,
                                Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))
                            ));
                        }
                    }
                }
            }
        }
    }
}
