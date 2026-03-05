package xaeroplus.util.normalizer;

import xaeroplus.XaeroPlus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * File-based IPC between the normalizer and the in-client hot-reload handler.
 *
 * <p>The normalizer writes a signal file after converting region files. The companion
 * hot-reload tick handler polls for this file and schedules render-thread reloads.</p>
 *
 * <p>Signal file format: one line per region with space-separated fields:</p>
 * <pre>dimId regionX regionZ</pre>
 *
 * <p>Example:</p>
 * <pre>
 * dim%0 0 0
 * dim%0 1 -1
 * dim%-1 3 4
 * </pre>
 */
public final class ReloadSignal {

    /** Signal file name, placed directly in the XaeroWorldMap save folder. */
    public static final String SIGNAL_FILE_NAME = ".normalizer_reload";

    /**
     * A single reload request for one region.
     */
    public record ReloadRequest(String dimId, int regionX, int regionZ) {
        @Override
        public String toString() {
            return dimId + " " + regionX + " " + regionZ;
        }

        /** Parse a single line into a request. Returns null if malformed. */
        public static ReloadRequest parse(String line) {
            if (line == null || line.isBlank()) return null;
            String[] parts = line.trim().split("\\s+");
            if (parts.length < 3) return null;
            try {
                return new ReloadRequest(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    private ReloadSignal() {}

    // ===================================================================
    //  Writer side (used by normalizer after conversion)
    // ===================================================================

    /**
     * Append reload requests to the signal file.
     *
     * <p>Thread-safe: uses {@link Files#write} with {@link StandardOpenOption#APPEND}
     * and {@link StandardOpenOption#CREATE}.</p>
     *
     * @param saveFolder the XaeroWorldMap save folder containing the world data
     * @param requests   regions that were converted and need reload
     */
    public static void writeRequests(Path saveFolder, List<ReloadRequest> requests) throws IOException {
        if (requests.isEmpty()) return;
        Path signalFile = saveFolder.resolve(SIGNAL_FILE_NAME);
        List<String> lines = new ArrayList<>(requests.size());
        for (ReloadRequest r : requests) {
            lines.add(r.toString());
        }
        Files.write(signalFile, lines, StandardCharsets.UTF_8,
            StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE);
    }

    /**
     * Write a single reload request.
     */
    public static void writeRequest(Path saveFolder, String dimId, int regionX, int regionZ) throws IOException {
        writeRequests(saveFolder, List.of(new ReloadRequest(dimId, regionX, regionZ)));
    }

    // ===================================================================
    //  Reader side (used by companion mod tick handler)
    // ===================================================================

    /**
     * Read and atomically consume the signal file.
     *
     * <p>Reads the file contents, then deletes it. If no signal file exists,
     * returns an empty list.</p>
     *
     * @param saveFolder the XaeroWorldMap save folder to check
     * @return list of reload requests, or empty if no signal file
     */
    public static List<ReloadRequest> consumeRequests(Path saveFolder) {
        Path signalFile = saveFolder.resolve(SIGNAL_FILE_NAME);
        if (!Files.exists(signalFile)) {
            return Collections.emptyList();
        }
        try {
            List<String> lines = Files.readAllLines(signalFile, StandardCharsets.UTF_8);
            // Delete immediately after reading to minimize window for duplicate reads
            Files.deleteIfExists(signalFile);

            List<ReloadRequest> requests = new ArrayList<>();
            for (String line : lines) {
                ReloadRequest r = ReloadRequest.parse(line);
                if (r != null) {
                    requests.add(r);
                }
            }
            return requests;
        } catch (IOException e) {
            XaeroPlus.LOGGER.warn("[HotReload] Failed to consume signal file: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
