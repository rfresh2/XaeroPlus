package xaeroplus.util.normalizer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import xaeroplus.util.normalizer.NormalizedRegion.*;

import java.io.*;
import java.util.*;

/**
 * Serializes a {@link NormalizedRegion} back into Xaero's binary region format
 * at any target version (major 0–7, minor 0–8).
 *
 * <p>Handles palette construction, height encoding, overlay opacity encoding,
 * and all version-conditional fields. Applies reverse block/biome fixers when
 * backporting to older versions and logs losses via {@link LossReport}.</p>
 */
public class RegionBinaryWriter {

    private final int targetMajor;
    private final int targetMinor;
    private final boolean is115flag;
    private final LossReport report;

    // For backporting to major 0: state string → numeric stateId
    private final Map<String, Integer> vanillaStatesReverse;

    // Fallback numeric state ID for blocks that can't be reverse-mapped (stone)
    private static final int STONE_STATE_ID = 1; // blockId=1, meta=0

    /**
     * @param targetMajor       target format major version (0–7)
     * @param targetMinor       target format minor version (0–8)
     * @param is115flag         is115 flag value (only relevant for major 2, minor >= 5)
     * @param report            loss report to track lossy operations
     * @param vanillaStatesReverse reverse lookup for major 0 backporting (state string → stateId).
     *                           May be null/empty if not backporting to major 0.
     */
    public RegionBinaryWriter(int targetMajor, int targetMinor, boolean is115flag,
                              LossReport report, Map<String, Integer> vanillaStatesReverse) {
        this.targetMajor = targetMajor;
        this.targetMinor = targetMinor;
        this.is115flag = is115flag;
        this.report = report != null ? report : new LossReport();
        this.vanillaStatesReverse = vanillaStatesReverse != null ? vanillaStatesReverse : Map.of();
    }

    /**
     * Write a normalized region to a DataOutputStream in the target format version.
     */
    public void write(DataOutputStream out, NormalizedRegion region) throws IOException {
        // Per-region palettes
        Map<String, Integer> blockPalette = new LinkedHashMap<>();
        List<CompoundTag> blockPaletteTags = new ArrayList<>();
        Map<String, Integer> biomePalette = new LinkedHashMap<>();

        boolean stillUsesColorTypes = targetMinor < 5 || (targetMajor <= 2 && !is115flag);

        // ===== Header =====
        if (targetMajor > 0 || targetMinor > 0) {
            out.writeByte(0xFF);
            int fullVersion = (targetMajor << 16) | targetMinor;
            out.writeInt(fullVersion);
            if (targetMajor == 2 && targetMinor >= 5) {
                out.writeByte(is115flag ? 1 : 0);
            }
        }
        // Major 0 has no header marker — first byte is chunk coord

        // ===== Chunk loop =====
        for (NormalizedChunk chunk : region.chunks()) {
            int chunkCoordByte = ((chunk.cx() & 0xF) << 4) | (chunk.cz() & 0xF);
            out.writeByte(chunkCoordByte);

            writeChunkTiles(out, chunk, stillUsesColorTypes, blockPalette, blockPaletteTags, biomePalette);
        }
        // EOF signals end of region (stream close)
    }

    // ========== Tile Writing ==========

    private void writeChunkTiles(
        DataOutputStream out, NormalizedChunk chunk, boolean stillUsesColorTypes,
        Map<String, Integer> blockPalette, List<CompoundTag> blockPaletteTags,
        Map<String, Integer> biomePalette
    ) throws IOException {
        // Build a lookup for present tiles
        Map<Long, NormalizedTile> tileMap = new HashMap<>();
        for (NormalizedTile tile : chunk.tiles()) {
            tileMap.put(((long) tile.tx() << 32) | tile.tz(), tile);
        }

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                NormalizedTile tile = tileMap.get(((long) i << 32) | j);
                if (tile == null || tile.blocks().isEmpty()) {
                    out.writeInt(-1); // tile absent
                    continue;
                }

                // Write 256 pixels — first pixel's parametres serves as the tile marker
                writeTilePixels(out, tile, stillUsesColorTypes, blockPalette, blockPaletteTags, biomePalette);

                // Tile metadata
                if (targetMinor >= 4) {
                    out.writeByte(tile.worldInterpretationVersion());
                } else {
                    reportLossIfPresent("worldInterpretationVersion",
                        tile.worldInterpretationVersion(), 0);
                }

                if (targetMinor >= 6) {
                    out.writeInt(tile.caveStart());
                    if (targetMinor >= 7) {
                        out.writeByte(tile.caveDepth());
                    } else {
                        reportLossIfPresent("caveDepth", tile.caveDepth(), 32);
                    }
                } else {
                    reportLossIfPresent("caveStart", tile.caveStart(), Integer.MAX_VALUE);
                }
            }
        }
    }

    // ========== Pixel Writing ==========

    private void writeTilePixels(
        DataOutputStream out, NormalizedTile tile, boolean stillUsesColorTypes,
        Map<String, Integer> blockPalette, List<CompoundTag> blockPaletteTags,
        Map<String, Integer> biomePalette
    ) throws IOException {
        // Build a lookup for blocks by position
        Map<Long, NormalizedBlock> blockMap = new HashMap<>();
        for (NormalizedBlock block : tile.blocks()) {
            blockMap.put(((long) block.x() << 32) | block.z(), block);
        }

        boolean first = true;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                NormalizedBlock block = blockMap.get(((long) x << 32) | z);
                if (block == null) {
                    // Synthesize empty block
                    block = new NormalizedBlock(x, z, "minecraft:air", false, 0, 0,
                        null, 0, false, 0, 0, true, List.of());
                }

                // Determine if this block state is new to the palette
                boolean isNotGrass = !"minecraft:grass_block".equals(block.state());
                boolean isBlockPaletteNew = false;
                if (isNotGrass && targetMajor > 0) {
                    isBlockPaletteNew = !blockPalette.containsKey(block.state());
                }

                boolean hasBiome = block.biome() != null;
                boolean isBiomePaletteNew = false;
                boolean biomeAsInt = false;
                if (hasBiome && targetMajor >= 4) {
                    isBiomePaletteNew = !biomePalette.containsKey(block.biome());
                    // We always write biomes as strings (not int), so biomeAsInt = false
                }

                boolean hasOverlays = block.overlays() != null && !block.overlays().isEmpty();
                boolean topHeightIsDifferent = targetMinor >= 4 && block.topHeight() != block.height();

                // --- Encode height ---
                int height = block.height();
                boolean useHeightAsByte;
                int heightLow, heightHigh;

                if (targetMinor >= 4) {
                    // 12-bit signed height
                    int clamped = Math.max(-2048, Math.min(2047, height));
                    int unsigned12 = clamped & 0xFFF;
                    heightLow = unsigned12 & 0xFF;
                    heightHigh = (unsigned12 >> 8) & 0xF;
                    useHeightAsByte = false;
                } else {
                    // Older formats: 9-bit signed or byte
                    if (height < 0 || height > 255) {
                        // Must use 9-bit encoding
                        int clamped9 = Math.max(-256, Math.min(255, height));
                        if (height != clamped9) {
                            report.warn("height", "Height " + height + " clamped to " + clamped9
                                + " (9-bit range for minor < 4)");
                        }
                        int unsigned9 = clamped9 & 0x1FF;
                        heightLow = unsigned9 & 0xFF;
                        heightHigh = (unsigned9 >> 8) & 1;
                        useHeightAsByte = false;
                    } else {
                        // Fits in unsigned byte
                        heightLow = height & 0xFF;
                        heightHigh = 0;
                        useHeightAsByte = true;
                    }
                }

                // --- Build parametres int ---
                int parametres = 0;
                if (isNotGrass)       parametres |= 1;
                if (hasOverlays)      parametres |= (1 << 1);
                // savedColourType in bits 2-3: always 0 for modern output
                if (useHeightAsByte)  parametres |= (1 << 6);
                parametres |= (block.light() & 0xF) << 8;
                parametres |= (heightLow & 0xFF) << 12;
                if (hasBiome)         parametres |= (1 << 20);
                if (isBlockPaletteNew && targetMajor > 0) parametres |= (1 << 21);
                if (isBiomePaletteNew && targetMajor >= 4) parametres |= (1 << 22);
                if (biomeAsInt && targetMajor >= 4)        parametres |= (1 << 23);

                if (targetMinor >= 4) {
                    if (topHeightIsDifferent) parametres |= (1 << 24);
                    parametres |= (heightHigh & 0xF) << 25;
                } else {
                    parametres |= (heightHigh & 1) << 24;
                }

                // Slope flag (minor == 2 only, if verticalSlope != 0)
                if (targetMinor == 2 && block.verticalSlope() != 0) {
                    parametres |= (1 << 4);
                }

                out.writeInt(parametres);

                // --- Block state ---
                if (isNotGrass) {
                    writeBlockState(out, block.state(), block.unknown(), isBlockPaletteNew,
                        blockPalette, blockPaletteTags);
                }

                // --- Height (if heightAsByte) ---
                if (useHeightAsByte) {
                    out.writeByte(height & 0xFF);
                }

                // --- Top height ---
                if (topHeightIsDifferent && targetMinor >= 4) {
                    out.writeByte(block.topHeight());
                }

                // --- Overlays ---
                if (hasOverlays) {
                    writeOverlays(out, block.overlays(), blockPalette, blockPaletteTags);
                }

                // --- Biome ---
                if (hasBiome) {
                    writeBiome(out, block.biome(), isBiomePaletteNew, biomeAsInt, biomePalette);
                }

                // --- Slope (minor == 2 only) ---
                if (targetMinor == 2 && block.verticalSlope() != 0) {
                    out.writeByte(block.verticalSlope());
                }

                first = false;
            }
        }
    }

    // ========== Block State Writing ==========

    private void writeBlockState(
        DataOutputStream out, String state, boolean unknown,
        boolean isPaletteNew,
        Map<String, Integer> blockPalette, List<CompoundTag> blockPaletteTags
    ) throws IOException {
        if (targetMajor == 0) {
            // Major 0: write numeric state ID
            writeNumericStateId(out, state);
            return;
        }

        if (isPaletteNew) {
            // New palette entry: write full CompoundTag
            CompoundTag tag = BlockStateFixers.stringToTag(state);
            // Apply reverse fixers for backporting
            BlockStateFixers.applyReverse(tag, targetMajor, report);
            NbtIo.write(tag, out);
            blockPalette.put(state, blockPaletteTags.size());
            blockPaletteTags.add(tag);
        } else {
            // Existing palette entry: write index
            Integer index = blockPalette.get(state);
            out.writeInt(index != null ? index : 0);
        }
    }

    private void writeNumericStateId(DataOutputStream out, String state) throws IOException {
        // Try reverse lookup in vanilla_states.dat
        Integer stateId = vanillaStatesReverse.get(state);
        if (stateId != null) {
            out.writeInt(stateId);
        } else {
            // Block doesn't exist in 1.12.2 — substitute stone
            report.error("block-backport",
                "Block '" + state + "' has no numeric ID (post-1.12.2 block); substituted stone");
            out.writeInt(STONE_STATE_ID);
        }
    }

    // ========== Overlay Writing ==========

    private void writeOverlays(
        DataOutputStream out, List<NormalizedOverlay> overlays,
        Map<String, Integer> blockPalette, List<CompoundTag> blockPaletteTags
    ) throws IOException {
        out.writeByte(overlays.size());

        for (NormalizedOverlay overlay : overlays) {
            boolean isNotWater = !"minecraft:water".equals(overlay.state());
            boolean overlayPaletteNew = false;
            if (isNotWater && targetMajor > 0) {
                overlayPaletteNew = !blockPalette.containsKey(overlay.state());
            }

            int overlayParams = 0;
            if (isNotWater)        overlayParams |= 1;
            overlayParams |= (overlay.light() & 0xF) << 4;
            if (overlayPaletteNew) overlayParams |= (1 << 10);

            // Opacity encoding
            if (targetMinor >= 8) {
                int opacity4 = Math.min(15, Math.max(0, overlay.opacity()));
                if (overlay.opacity() > 15) {
                    report.warn("overlay-opacity",
                        "Opacity " + overlay.opacity() + " clamped to 15 (4-bit for minor >= 8)");
                }
                overlayParams |= (opacity4 & 0xF) << 11;
            } else {
                // minor < 8: opacity written separately if != 1
                if (overlay.opacity() != 1) {
                    overlayParams |= 8; // flag: full int opacity follows
                }
            }

            out.writeInt(overlayParams);

            // Overlay block state
            if (isNotWater) {
                writeBlockState(out, overlay.state(), false, overlayPaletteNew,
                    blockPalette, blockPaletteTags);
            }

            // Opacity (minor < 8, if flagged)
            if (targetMinor < 8 && (overlayParams & 8) != 0) {
                out.writeInt(overlay.opacity());
            }
        }
    }

    // ========== Biome Writing ==========

    private void writeBiome(
        DataOutputStream out, String biome,
        boolean isPaletteNew, boolean asInt,
        Map<String, Integer> biomePalette
    ) throws IOException {
        // Apply reverse biome renames for backporting
        biome = BiomeTable.applyReverseRename(biome, targetMajor, report);

        if (targetMajor < 4) {
            // Legacy numeric biome
            int biomeId = BiomeTable.toId(biome);
            if (biomeId == -1) {
                report.warn("biome-backport",
                    "Biome '" + biome + "' has no numeric ID; substituted plains (1)");
                biomeId = 1; // plains
            }

            if (targetMinor >= 3 && biomeId >= 255) {
                out.writeByte(255); // signal extended
                out.writeInt(biomeId);
            } else {
                out.writeByte(biomeId & 0xFF);
            }
        } else {
            // Modern biome (major >= 4)
            if (isPaletteNew) {
                // We always use string encoding (not int)
                out.writeUTF(biome);
                biomePalette.put(biome, biomePalette.size());
            } else {
                Integer index = biomePalette.get(biome);
                out.writeInt(index != null ? index : 0);
            }
        }
    }

    // ========== Helpers ==========

    private void reportLossIfPresent(String field, int value, int defaultValue) {
        if (value != defaultValue) {
            report.warn("tile-metadata",
                field + "=" + value + " discarded (not supported at target version "
                    + targetMajor + "." + targetMinor + ")");
        }
    }
}
