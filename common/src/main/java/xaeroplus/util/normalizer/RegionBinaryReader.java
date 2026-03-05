package xaeroplus.util.normalizer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import xaeroplus.util.normalizer.NormalizedRegion.*;

import java.io.*;
import java.util.*;

/**
 * Parses the binary format of a Xaero's World Map region file.
 *
 * <p>Handles all format versions from major 0 (pre-1.13) through major 7 minor 8 (current).
 * Block and biome palettes are scoped per-region. The block palette is shared between
 * base block reads and overlay reads.</p>
 */
public class RegionBinaryReader {

    /** Result of reading a region file's binary content. */
    public record ReadResult(int majorVersion, int minorVersion, List<NormalizedChunk> chunks) {}

    private final Map<Integer, CompoundTag> vanillaStates;

    /**
     * @param vanillaStates map from legacy stateId to CompoundTag (major 0 files). May be empty.
     */
    public RegionBinaryReader(Map<Integer, CompoundTag> vanillaStates) {
        this.vanillaStates = vanillaStates != null ? vanillaStates : Map.of();
    }

    /**
     * Read a region from a DataInputStream.
     */
    public ReadResult read(DataInputStream in) throws IOException {
        List<CompoundTag> blockPalette = new ArrayList<>();
        List<String> biomePalette = new ArrayList<>();

        // ===== Header =====
        int majorVersion, minorVersion;
        boolean is115flag = false;
        int pendingChunkCoord = -1;

        int firstByte = in.read();
        if (firstByte == -1) {
            return new ReadResult(0, 0, List.of());
        }

        if (firstByte == 0xFF) {
            int fullVersion = in.readInt();
            majorVersion = fullVersion >> 16;
            minorVersion = fullVersion & 0xFFFF;
            if (majorVersion == 2 && minorVersion >= 5) {
                is115flag = in.readByte() == 1;
            }
        } else {
            majorVersion = 0;
            minorVersion = 0;
            pendingChunkCoord = firstByte;
        }

        boolean stillUsesColorTypes = minorVersion < 5 || (majorVersion <= 2 && !is115flag);

        // ===== Chunk loop =====
        List<NormalizedChunk> chunks = new ArrayList<>();

        while (true) {
            int chunkCoordByte;
            if (pendingChunkCoord >= 0) {
                chunkCoordByte = pendingChunkCoord;
                pendingChunkCoord = -1;
            } else {
                chunkCoordByte = in.read();
            }
            if (chunkCoordByte == -1) break;

            int chunkX = (chunkCoordByte >> 4) & 0xF;
            int chunkZ = chunkCoordByte & 0xF;

            List<NormalizedTile> tiles = readChunkTiles(
                in, majorVersion, minorVersion, is115flag, stillUsesColorTypes,
                blockPalette, biomePalette
            );

            chunks.add(new NormalizedChunk(chunkX, chunkZ, tiles));
        }

        return new ReadResult(majorVersion, minorVersion, chunks);
    }

    // ========== Tile Reading ==========

    private List<NormalizedTile> readChunkTiles(
        DataInputStream in, int major, int minor, boolean is115flag, boolean stillUsesColorTypes,
        List<CompoundTag> blockPalette, List<String> biomePalette
    ) throws IOException {
        List<NormalizedTile> tiles = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                int marker = in.readInt();
                if (marker == -1) continue; // tile absent

                List<NormalizedBlock> blocks = readTilePixels(
                    in, major, minor, is115flag, stillUsesColorTypes,
                    blockPalette, biomePalette, marker
                );

                int worldInterpretationVersion = 0;
                if (minor >= 4) {
                    worldInterpretationVersion = in.readByte() & 0xFF;
                }

                int caveStart = Integer.MAX_VALUE;
                int caveDepth = 32;
                if (minor >= 6) {
                    caveStart = in.readInt();
                    if (minor >= 7) {
                        caveDepth = in.readByte() & 0xFF;
                    }
                }

                tiles.add(new NormalizedTile(i, j, worldInterpretationVersion, caveStart, caveDepth, blocks));
            }
        }

        return tiles;
    }

    // ========== Pixel Reading ==========

    private List<NormalizedBlock> readTilePixels(
        DataInputStream in, int major, int minor, boolean is115flag, boolean stillUsesColorTypes,
        List<CompoundTag> blockPalette, List<String> biomePalette, int firstParametres
    ) throws IOException {
        List<NormalizedBlock> blocks = new ArrayList<>(256);
        boolean first = true;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int parametres = first ? firstParametres : in.readInt();
                first = false;

                blocks.add(readPixel(
                    in, major, minor, is115flag, stillUsesColorTypes,
                    blockPalette, biomePalette, parametres, x, z
                ));
            }
        }

        return blocks;
    }

    private NormalizedBlock readPixel(
        DataInputStream in, int major, int minor, boolean is115flag, boolean stillUsesColorTypes,
        List<CompoundTag> blockPalette, List<String> biomePalette,
        int parametres, int x, int z
    ) throws IOException {
        // --- Unpack parametres ---
        boolean isNotGrass       = (parametres & 1) != 0;
        boolean hasOverlays      = (parametres & 2) != 0;
        int savedColourType      = stillUsesColorTypes ? (parametres >> 2) & 3 : 0;
        boolean heightAsByte     = (parametres & (1 << 6)) != 0;
        int light                = (parametres >> 8) & 0xF;
        int heightLow            = (parametres >> 12) & 0xFF;
        boolean hasBiome         = (parametres & (1 << 20)) != 0;
        boolean blockPaletteNew  = (parametres & (1 << 21)) != 0;
        boolean biomePaletteNew  = (parametres & (1 << 22)) != 0;
        boolean biomeAsInt       = (parametres & (1 << 23)) != 0;

        boolean topHeightIsDifferent;
        int heightHigh;
        if (minor >= 4) {
            topHeightIsDifferent = (parametres & (1 << 24)) != 0;
            heightHigh = (parametres >> 25) & 0xF;
        } else {
            topHeightIsDifferent = false;
            heightHigh = (parametres >> 24) & 1;
        }

        // --- Block state ---
        String state;
        boolean unknown = false;
        if (!isNotGrass) {
            state = "minecraft:grass_block";
        } else {
            var blockResult = readBlockState(in, major, blockPaletteNew, blockPalette);
            state = blockResult.state;
            unknown = blockResult.unknown;
        }

        // --- Height ---
        int height;
        if (heightAsByte) {
            height = in.readByte() & 0xFF;
        } else if (minor >= 4) {
            int combined = heightLow | (heightHigh << 8);
            height = (combined << 20) >> 20; // sign-extend 12-bit
        } else {
            int combined = heightLow | (heightHigh << 8);
            height = (combined << 23) >> 23; // sign-extend 9-bit
        }

        // --- Top height ---
        int topHeight;
        if (topHeightIsDifferent && minor >= 4) {
            topHeight = in.readByte();
        } else {
            topHeight = height;
        }

        // --- Overlays ---
        List<NormalizedOverlay> overlays;
        if (hasOverlays) {
            overlays = readOverlays(in, major, minor, is115flag, stillUsesColorTypes, blockPalette);
        } else {
            overlays = List.of();
        }

        // --- Biome ---
        String biome = null;
        if (hasBiome || savedColourType == 1 || savedColourType == 2) {
            biome = readBiome(in, major, minor, biomePaletteNew, biomeAsInt, biomePalette);
        }

        // --- ColourType discard ---
        if (savedColourType == 3) {
            in.readInt(); // discard custom color
        }

        // --- Slope (minor == 2 only) ---
        int verticalSlope = 0;
        if (minor == 2 && (parametres & 16) != 0) {
            verticalSlope = in.readByte();
        }

        return new NormalizedBlock(
            x, z, state, unknown, height, topHeight,
            biome, light, false, verticalSlope, 0, true,
            overlays
        );
    }

    // ========== Block State ==========

    private record BlockReadResult(String state, boolean unknown) {}

    private BlockReadResult readBlockState(
        DataInputStream in, int major, boolean paletteNew, List<CompoundTag> blockPalette
    ) throws IOException {
        if (major == 0) {
            int stateId = in.readInt();
            CompoundTag tag = vanillaStates.get(stateId);
            if (tag != null) {
                tag = tag.copy();
                BlockStateFixers.applyForward(tag, major);
                return new BlockReadResult(BlockStateFixers.tagToString(tag), false);
            }
            return new BlockReadResult(
                "minecraft:unknown[legacyId=" + (stateId & 0xFFF) + ",meta=" + ((stateId >> 12) & 0xFFFFF) + "]",
                true
            );
        }

        if (paletteNew) {
            CompoundTag tag = NbtIo.read(in);
            boolean unknown = false;
            if (major < 7) {
                BlockStateFixers.applyForward(tag, major);
            }
            blockPalette.add(tag);
            String stateStr = BlockStateFixers.tagToString(tag);
            // Flag as unknown if the Name contains a non-minecraft namespace
            if (!stateStr.startsWith("minecraft:")) {
                unknown = true;
            }
            return new BlockReadResult(stateStr, unknown);
        } else {
            int paletteIndex = in.readInt();
            if (paletteIndex >= 0 && paletteIndex < blockPalette.size()) {
                CompoundTag tag = blockPalette.get(paletteIndex);
                String stateStr = BlockStateFixers.tagToString(tag);
                return new BlockReadResult(stateStr, !stateStr.startsWith("minecraft:"));
            }
            return new BlockReadResult("minecraft:unknown[paletteIndex=" + paletteIndex + "]", true);
        }
    }

    // ========== Overlays ==========

    private List<NormalizedOverlay> readOverlays(
        DataInputStream in, int major, int minor, boolean is115flag, boolean stillUsesColorTypes,
        List<CompoundTag> blockPalette
    ) throws IOException {
        int overlayCount = in.readByte() & 0xFF;
        List<NormalizedOverlay> overlays = new ArrayList<>(overlayCount);

        for (int i = 0; i < overlayCount; i++) {
            int overlayParams = in.readInt();

            boolean isNotWater        = (overlayParams & 1) != 0;
            int overlayLight          = (overlayParams >> 4) & 0xF;
            boolean overlayPaletteNew = (overlayParams & (1 << 10)) != 0;

            String overlayState;
            if (!isNotWater) {
                overlayState = "minecraft:water";
            } else {
                var result = readBlockState(in, major, overlayPaletteNew, blockPalette);
                overlayState = result.state;
            }

            // Legacy color data discard
            if (minor < 1 && (overlayParams & 2) != 0) {
                in.readInt();
            }

            int overlaySavedColourType = stillUsesColorTypes ? (overlayParams >> 8) & 3 : 0;
            if (overlaySavedColourType == 2 || (overlayParams & 4) != 0) {
                in.readInt(); // discard custom color
            }

            // Opacity
            int opacity;
            if (minor < 8) {
                if ((overlayParams & 8) != 0) {
                    opacity = in.readInt();
                } else {
                    opacity = 1;
                }
            } else {
                opacity = (overlayParams >> 11) & 0xF;
            }

            overlays.add(new NormalizedOverlay(overlayState, overlayLight, opacity));
        }

        return overlays;
    }

    // ========== Biome ==========

    private String readBiome(
        DataInputStream in, int major, int minor,
        boolean biomePaletteNew, boolean biomeAsInt, List<String> biomePalette
    ) throws IOException {
        String biome;

        if (major < 4) {
            int biomeByte = in.readByte() & 0xFF;
            int biomeId;
            if (minor >= 3 && biomeByte >= 255) {
                biomeId = in.readInt();
            } else {
                biomeId = biomeByte;
            }
            biome = BiomeTable.fromId(biomeId);
        } else {
            if (biomePaletteNew) {
                if (biomeAsInt) {
                    int biomeId = in.readInt();
                    biome = BiomeTable.fromId(biomeId);
                } else {
                    biome = in.readUTF();
                }
                biome = BiomeTable.ensureNamespaced(biome);
                biomePalette.add(biome);
            } else {
                int paletteIndex = in.readInt();
                if (paletteIndex >= 0 && paletteIndex < biomePalette.size()) {
                    biome = biomePalette.get(paletteIndex);
                } else {
                    biome = "minecraft:unknown_biome[paletteIndex=" + paletteIndex + "]";
                }
            }
        }

        // Apply forward renames (Caves & Cliffs)
        biome = BiomeTable.applyForwardRename(biome, major);

        return biome;
    }
}
