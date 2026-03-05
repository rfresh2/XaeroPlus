package xaeroplus.util.normalizer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;

/**
 * Canonical, version-independent representation of a Xaero's World Map region.
 * All block states and biomes are fully resolved and fixed to modern identifiers.
 */
public record NormalizedRegion(
    String format,
    String worldId,
    String dimId,
    String mwId,
    int caveLayer,
    int regionX,
    int regionZ,
    SourceVersion sourceVersion,
    List<NormalizedChunk> chunks
) {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public String toJson() {
        return GSON.toJson(this);
    }

    public record SourceVersion(int major, int minor) {}

    public record NormalizedChunk(int cx, int cz, List<NormalizedTile> tiles) {}

    public record NormalizedTile(
        int tx, int tz,
        int worldInterpretationVersion,
        int caveStart,
        int caveDepth,
        List<NormalizedBlock> blocks
    ) {}

    public record NormalizedBlock(
        int x, int z,
        String state,
        boolean unknown,
        int height,
        int topHeight,
        String biome,
        int light,
        boolean glowing,
        int verticalSlope,
        int diagonalSlope,
        boolean slopeUnknown,
        List<NormalizedOverlay> overlays
    ) {}

    public record NormalizedOverlay(String state, int light, int opacity) {}
}
