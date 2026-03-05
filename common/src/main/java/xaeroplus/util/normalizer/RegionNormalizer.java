package xaeroplus.util.normalizer;

import net.minecraft.nbt.CompoundTag;
import xaeroplus.XaeroPlus;
import xaeroplus.util.normalizer.NormalizedRegion.SourceVersion;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Main entry point for the Xaero's World Map cross-version data normalizer.
 *
 * <p>Reads region files from any format version (MC 1.12 through 1.21.5+),
 * produces a canonical {@link NormalizedRegion}, and can re-serialize
 * to any target Xaero format version.</p>
 *
 * <h3>Read (forward-port to canonical):</h3>
 * <pre>{@code
 * RegionNormalizer normalizer = new RegionNormalizer();
 * NormalizedRegion region = normalizer.read(
 *     Path.of("0_0.zip"), "myWorld", "dim%0", null, Integer.MAX_VALUE, 0, 0
 * );
 * System.out.println(region.toJson());
 * }</pre>
 *
 * <h3>Write (back-port / forward-port to specific version):</h3>
 * <pre>{@code
 * LossReport report = new LossReport();
 * normalizer.write(region, Path.of("0_0.zip"), 7, 8, false, report);
 * if (report.hasLosses()) report.dump();
 * }</pre>
 *
 * <h3>Convert (read + write in one step):</h3>
 * <pre>{@code
 * LossReport report = normalizer.convert(
 *     Path.of("old/0_0.zip"), "world", "dim%0", null, Integer.MAX_VALUE, 0, 0,
 *     Path.of("new/0_0.zip"), 7, 8, false
 * );
 * }</pre>
 */
public class RegionNormalizer {

    private static final String FORMAT_ID = "xaero-normalized-v1";

    private final Map<Integer, CompoundTag> vanillaStates;
    private final Map<String, Integer> vanillaStatesReverse;
    private final RegionBinaryReader reader;

    /** Create a normalizer, loading vanilla_states.dat from the classpath. */
    public RegionNormalizer() {
        this(VanillaStatesLoader.loadFromClasspath());
    }

    /** Create a normalizer with a pre-loaded vanilla states map. */
    public RegionNormalizer(Map<Integer, CompoundTag> vanillaStates) {
        this.vanillaStates = vanillaStates;
        this.vanillaStatesReverse = VanillaStatesLoader.buildReverseLookup(vanillaStates);
        this.reader = new RegionBinaryReader(vanillaStates);
    }

    // ===================================================================
    //  READ — any version → canonical NormalizedRegion
    // ===================================================================

    /**
     * Read and normalize a single region file.
     */
    public NormalizedRegion read(
        Path regionFile, String worldId, String dimId,
        String mwId, int caveLayer, int regionX, int regionZ
    ) throws IOException {
        byte[] data = extractBinaryData(regionFile);
        RegionBinaryReader.ReadResult result;
        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data))) {
            result = reader.read(dis);
        }
        return new NormalizedRegion(
            FORMAT_ID, worldId, dimId, mwId, caveLayer, regionX, regionZ,
            new SourceVersion(result.majorVersion(), result.minorVersion()),
            result.chunks()
        );
    }

    /** Read using metadata from a {@link DirectoryWalker.RegionFileInfo}. */
    public NormalizedRegion read(DirectoryWalker.RegionFileInfo info) throws IOException {
        return read(info.path(), info.worldId(), info.dimId(),
            info.mwId(), info.caveLayer(), info.regionX(), info.regionZ());
    }

    // ===================================================================
    //  WRITE — canonical NormalizedRegion → target version file
    // ===================================================================

    /**
     * Write a normalized region to a file at the specified target version.
     *
     * @param region      the normalized region data
     * @param outputFile  output path (.zip or .xaero)
     * @param targetMajor target format major version (0–7)
     * @param targetMinor target format minor version (0–8)
     * @param is115flag   is115 flag (only relevant for major 2, minor >= 5)
     * @param report      loss report to track lossy backport operations
     */
    public void write(NormalizedRegion region, Path outputFile,
                      int targetMajor, int targetMinor, boolean is115flag,
                      LossReport report) throws IOException {
        write(region, outputFile, targetMajor, targetMinor, is115flag, report, false);
    }

    /**
     * Write a normalized region to a file at the specified target version,
     * optionally using atomic file replacement.
     *
     * <p>Atomic write: data is written to {@code <target>.tmp}, then renamed
     * via {@link Files#move} with {@link StandardCopyOption#ATOMIC_MOVE}.
     * Falls back to {@link StandardCopyOption#REPLACE_EXISTING} if atomic
     * move is not supported by the filesystem.</p>
     *
     * @param region      the normalized region data
     * @param outputFile  output path (.zip or .xaero)
     * @param targetMajor target format major version (0–7)
     * @param targetMinor target format minor version (0–8)
     * @param is115flag   is115 flag (only relevant for major 2, minor >= 5)
     * @param report      loss report to track lossy backport operations
     * @param atomic      true to use atomic temp-file-then-rename strategy
     */
    public void write(NormalizedRegion region, Path outputFile,
                      int targetMajor, int targetMinor, boolean is115flag,
                      LossReport report, boolean atomic) throws IOException {
        RegionBinaryWriter writer = new RegionBinaryWriter(
            targetMajor, targetMinor, is115flag, report, vanillaStatesReverse
        );

        // Serialize to bytes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream dos = new DataOutputStream(baos)) {
            writer.write(dos, region);
        }
        byte[] binaryData = baos.toByteArray();

        // Write to file
        Files.createDirectories(outputFile.getParent());

        if (atomic) {
            writeAtomic(outputFile, binaryData);
        } else {
            writeNormal(outputFile, binaryData);
        }
    }

    private void writeNormal(Path outputFile, byte[] binaryData) throws IOException {
        String fileName = outputFile.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".zip")) {
            writeToZip(outputFile, binaryData);
        } else {
            Files.write(outputFile, binaryData);
        }
    }

    /**
     * Atomic write: write to temp file, then rename.
     * Xaero never sees a half-written file.
     */
    private void writeAtomic(Path outputFile, byte[] binaryData) throws IOException {
        Path tempFile = outputFile.resolveSibling(outputFile.getFileName() + ".tmp");
        try {
            String fileName = outputFile.getFileName().toString().toLowerCase();
            if (fileName.endsWith(".zip")) {
                writeToZip(tempFile, binaryData);
            } else {
                Files.write(tempFile, binaryData);
            }
            // Attempt atomic move, fall back to replace
            try {
                Files.move(tempFile, outputFile,
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tempFile, outputFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            // Clean up temp file on failure
            try { Files.deleteIfExists(tempFile); } catch (IOException ignored) {}
            throw e;
        }
    }

    // ===================================================================
    //  CONVERT — read + write in one step
    // ===================================================================

    /**
     * Read a region file and re-serialize it at a target version.
     *
     * @return the loss report from the conversion
     */
    public LossReport convert(
        Path inputFile, String worldId, String dimId,
        String mwId, int caveLayer, int regionX, int regionZ,
        Path outputFile, int targetMajor, int targetMinor, boolean is115flag
    ) throws IOException {
        return convert(inputFile, worldId, dimId, mwId, caveLayer, regionX, regionZ,
            outputFile, targetMajor, targetMinor, is115flag, false, null);
    }

    /**
     * Read a region file and re-serialize it at a target version,
     * with atomic file replacement and optional reload signal.
     *
     * @param atomic       use atomic write (temp + rename)
     * @param signalFolder if non-null, write a reload signal to this folder
     *                     after successful conversion
     * @return the loss report from the conversion
     */
    public LossReport convert(
        Path inputFile, String worldId, String dimId,
        String mwId, int caveLayer, int regionX, int regionZ,
        Path outputFile, int targetMajor, int targetMinor, boolean is115flag,
        boolean atomic, Path signalFolder
    ) throws IOException {
        NormalizedRegion region = read(inputFile, worldId, dimId, mwId, caveLayer, regionX, regionZ);
        LossReport report = new LossReport();
        write(region, outputFile, targetMajor, targetMinor, is115flag, report, atomic);

        // Write reload signal for the companion hot-reload handler
        if (signalFolder != null) {
            try {
                ReloadSignal.writeRequest(signalFolder, dimId, regionX, regionZ);
            } catch (IOException e) {
                report.warn("signal", "Failed to write reload signal: " + e.getMessage());
            }
        }

        return report;
    }

    /**
     * Convert all region files in a save directory to the target version.
     *
     * @param inputFolder  source XaeroWorldMap save folder
     * @param outputFolder destination save folder (may be same as input for in-place)
     * @param targetMajor  target format major version
     * @param targetMinor  target format minor version
     * @param is115flag    is115 flag
     * @param invalidateCaches whether to delete .xwmc caches in the output folder
     * @return aggregated loss report
     */
    public LossReport convertDirectory(
        Path inputFolder, Path outputFolder,
        int targetMajor, int targetMinor, boolean is115flag,
        boolean invalidateCaches
    ) throws IOException {
        return convertDirectory(inputFolder, outputFolder, targetMajor, targetMinor,
            is115flag, invalidateCaches, false, null);
    }

    /**
     * Convert all region files in a save directory to the target version,
     * with atomic writes and optional hot-reload signaling.
     *
     * @param inputFolder      source XaeroWorldMap save folder
     * @param outputFolder     destination save folder (may be same as input for in-place)
     * @param targetMajor      target format major version
     * @param targetMinor      target format minor version
     * @param is115flag        is115 flag
     * @param invalidateCaches whether to delete .xwmc caches in the output folder
     * @param atomic           use atomic write (temp + rename) for each file
     * @param signalFolder     if non-null, write reload signals after each conversion
     * @return aggregated loss report
     */
    public LossReport convertDirectory(
        Path inputFolder, Path outputFolder,
        int targetMajor, int targetMinor, boolean is115flag,
        boolean invalidateCaches, boolean atomic, Path signalFolder
    ) throws IOException {
        List<DirectoryWalker.RegionFileInfo> files = DirectoryWalker.enumerate(inputFolder);
        LossReport aggregateReport = new LossReport();
        int converted = 0;
        int failed = 0;
        List<ReloadSignal.ReloadRequest> reloadRequests = new ArrayList<>();

        for (DirectoryWalker.RegionFileInfo info : files) {
            try {
                NormalizedRegion region = read(info);

                // Compute output path (mirror the directory structure)
                Path relativePath = inputFolder.relativize(info.path());
                Path outputFile = outputFolder.resolve(relativePath);
                // Ensure .zip extension
                if (!outputFile.toString().endsWith(".zip")) {
                    outputFile = outputFile.getParent().resolve(
                        outputFile.getFileName().toString().replaceAll("\\.xaero$", ".zip"));
                }

                LossReport report = new LossReport();
                write(region, outputFile, targetMajor, targetMinor, is115flag, report, atomic);

                // Copy losses into aggregate report
                for (LossReport.Entry entry : report.entries()) {
                    switch (entry.severity()) {
                        case INFO -> aggregateReport.info(entry.category(),
                            info.path().getFileName() + ": " + entry.message());
                        case WARNING -> aggregateReport.warn(entry.category(),
                            info.path().getFileName() + ": " + entry.message());
                        case ERROR -> aggregateReport.error(entry.category(),
                            info.path().getFileName() + ": " + entry.message());
                    }
                }

                // Track reload request
                if (signalFolder != null) {
                    reloadRequests.add(new ReloadSignal.ReloadRequest(
                        info.dimId(), info.regionX(), info.regionZ()));
                }

                converted++;
            } catch (Exception e) {
                aggregateReport.error("parse-error",
                    info.path() + ": " + e.getMessage());
                failed++;
            }
        }

        aggregateReport.info("summary",
            "Converted " + converted + " files, " + failed + " failed, "
                + aggregateReport.count(LossReport.Severity.WARNING) + " warnings");

        if (invalidateCaches) {
            try {
                List<Path> deleted = CacheInvalidator.invalidateAll(outputFolder);
                if (!deleted.isEmpty()) {
                    aggregateReport.info("cache",
                        "Deleted " + deleted.size() + " cache directories");
                }
            } catch (IOException e) {
                aggregateReport.error("cache", "Failed to invalidate caches: " + e.getMessage());
            }
        }

        // Write batch reload signals
        if (signalFolder != null && !reloadRequests.isEmpty()) {
            try {
                ReloadSignal.writeRequests(signalFolder, reloadRequests);
                aggregateReport.info("signal",
                    "Wrote " + reloadRequests.size() + " reload signals");
            } catch (IOException e) {
                aggregateReport.warn("signal",
                    "Failed to write reload signals: " + e.getMessage());
            }
        }

        return aggregateReport;
    }

    // ===================================================================
    //  BATCH READ — read all to canonical
    // ===================================================================

    /** Read all region files in a save directory to canonical form. */
    public List<NormalizedRegion> readDirectory(Path saveFolder) throws IOException {
        List<DirectoryWalker.RegionFileInfo> files = DirectoryWalker.enumerate(saveFolder);
        List<NormalizedRegion> regions = new ArrayList<>();
        for (DirectoryWalker.RegionFileInfo info : files) {
            try {
                regions.add(read(info));
            } catch (Exception e) {
                System.err.println("[XaeroNormalizer] Failed to parse " + info.path() + ": " + e.getMessage());
            }
        }
        return regions;
    }

    // ===================================================================
    //  File I/O Helpers
    // ===================================================================

    private byte[] extractBinaryData(Path regionFile) throws IOException {
        String fileName = regionFile.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".zip")) {
            return extractFromZip(regionFile);
        } else {
            return Files.readAllBytes(regionFile);
        }
    }

    private byte[] extractFromZip(Path zipFile) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if ("region.xaero".equals(entry.getName())) {
                    return zis.readAllBytes();
                }
            }
        }
        throw new IOException("ZIP does not contain region.xaero: " + zipFile);
    }

    private void writeToZip(Path zipFile, byte[] binaryData) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            zos.putNextEntry(new ZipEntry("region.xaero"));
            zos.write(binaryData);
            zos.closeEntry();
        }
    }
}
