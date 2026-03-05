package xaeroplus.util.normalizer;

import java.util.HashMap;
import java.util.Map;

/**
 * Numeric biome ID &harr; string tables and Caves &amp; Cliffs biome renames (forward + reverse).
 * All identifiers use the {@code minecraft:} namespace prefix.
 */
public final class BiomeTable {
    private BiomeTable() {}

    private static final Map<Integer, String> ID_TO_NAME = new HashMap<>();
    private static final Map<String, Integer> NAME_TO_ID = new HashMap<>();
    private static final Map<String, String> CAVES_CLIFFS_FORWARD = new HashMap<>();
    private static final Map<String, String> CAVES_CLIFFS_REVERSE = new HashMap<>();

    static {
        // ===== Numeric ID → String (175 entries) =====
        putBiome(0, "ocean");
        putBiome(1, "plains");
        putBiome(2, "desert");
        putBiome(3, "mountains");
        putBiome(4, "forest");
        putBiome(5, "taiga");
        putBiome(6, "swamp");
        putBiome(7, "river");
        putBiome(8, "nether_wastes");
        putBiome(9, "the_end");
        putBiome(10, "frozen_ocean");
        putBiome(11, "frozen_river");
        putBiome(12, "snowy_tundra");
        putBiome(13, "snowy_mountains");
        putBiome(14, "mushroom_fields");
        putBiome(15, "mushroom_field_shore");
        putBiome(16, "beach");
        putBiome(17, "desert_hills");
        putBiome(18, "wooded_hills");
        putBiome(19, "taiga_hills");
        putBiome(20, "mountain_edge");
        putBiome(21, "jungle");
        putBiome(22, "jungle_hills");
        putBiome(23, "jungle_edge");
        putBiome(24, "deep_ocean");
        putBiome(25, "stone_shore");
        putBiome(26, "snowy_beach");
        putBiome(27, "birch_forest");
        putBiome(28, "birch_forest_hills");
        putBiome(29, "dark_forest");
        putBiome(30, "snowy_taiga");
        putBiome(31, "snowy_taiga_hills");
        putBiome(32, "giant_tree_taiga");
        putBiome(33, "giant_tree_taiga_hills");
        putBiome(34, "wooded_mountains");
        putBiome(35, "savanna");
        putBiome(36, "savanna_plateau");
        putBiome(37, "badlands");
        putBiome(38, "wooded_badlands_plateau");
        putBiome(39, "badlands_plateau");
        putBiome(40, "small_end_islands");
        putBiome(41, "end_midlands");
        putBiome(42, "end_highlands");
        putBiome(43, "end_barrens");
        putBiome(44, "warm_ocean");
        putBiome(45, "lukewarm_ocean");
        putBiome(46, "cold_ocean");
        putBiome(47, "deep_warm_ocean");
        putBiome(48, "deep_lukewarm_ocean");
        putBiome(49, "deep_cold_ocean");
        putBiome(50, "deep_frozen_ocean");
        putBiome(127, "the_void");
        putBiome(129, "sunflower_plains");
        putBiome(130, "desert_lakes");
        putBiome(131, "gravelly_mountains");
        putBiome(132, "flower_forest");
        putBiome(133, "taiga_mountains");
        putBiome(134, "swamp_hills");
        putBiome(140, "ice_spikes");
        putBiome(149, "modified_jungle");
        putBiome(151, "modified_jungle_edge");
        putBiome(155, "tall_birch_forest");
        putBiome(156, "tall_birch_hills");
        putBiome(157, "dark_forest_hills");
        putBiome(158, "snowy_taiga_mountains");
        putBiome(160, "giant_spruce_taiga");
        putBiome(161, "giant_spruce_taiga_hills");
        putBiome(162, "modified_gravelly_mountains");
        putBiome(163, "shattered_savanna");
        putBiome(164, "shattered_savanna_plateau");
        putBiome(165, "eroded_badlands");
        putBiome(166, "modified_wooded_badlands_plateau");
        putBiome(167, "modified_badlands_plateau");
        putBiome(168, "bamboo_jungle");
        putBiome(169, "bamboo_jungle_hills");
        putBiome(170, "soul_sand_valley");
        putBiome(171, "crimson_forest");
        putBiome(172, "warped_forest");
        putBiome(173, "basalt_deltas");
        putBiome(174, "dripstone_caves");
        putBiome(175, "lush_caves");
        putBiome(177, "meadow");
        putBiome(178, "grove");
        putBiome(179, "snowy_slopes");
        putBiome(180, "snowcapped_peaks");
        putBiome(181, "lofty_peaks");
        putBiome(182, "stony_peaks");

        // ===== Caves & Cliffs forward renames (major < 6) =====
        putRename("badlands_plateau", "badlands");
        putRename("bamboo_jungle_hills", "bamboo_jungle");
        putRename("birch_forest_hills", "birch_forest");
        putRename("dark_forest_hills", "dark_forest");
        putRename("desert_hills", "desert");
        putRename("desert_lakes", "desert");
        putRename("giant_spruce_taiga_hills", "old_growth_spruce_taiga");
        putRename("giant_spruce_taiga", "old_growth_spruce_taiga");
        putRename("giant_tree_taiga_hills", "old_growth_pine_taiga");
        putRename("giant_tree_taiga", "old_growth_pine_taiga");
        putRename("gravelly_mountains", "windswept_gravelly_hills");
        putRename("jungle_edge", "sparse_jungle");
        putRename("jungle_hills", "jungle");
        putRename("modified_badlands_plateau", "badlands");
        putRename("modified_gravelly_mountains", "windswept_gravelly_hills");
        putRename("modified_jungle_edge", "sparse_jungle");
        putRename("modified_jungle", "jungle");
        putRename("modified_wooded_badlands_plateau", "wooded_badlands");
        putRename("mountain_edge", "windswept_hills");
        putRename("mountains", "windswept_hills");
        putRename("mushroom_field_shore", "mushroom_fields");
        putRename("shattered_savanna", "windswept_savanna");
        putRename("shattered_savanna_plateau", "windswept_savanna");
        putRename("snowy_mountains", "snowy_plains");
        putRename("snowy_taiga_hills", "snowy_taiga");
        putRename("snowy_taiga_mountains", "snowy_taiga");
        putRename("snowy_tundra", "snowy_plains");
        putRename("stone_shore", "stony_shore");
        putRename("swamp_hills", "swamp");
        putRename("taiga_hills", "taiga");
        putRename("taiga_mountains", "taiga");
        putRename("tall_birch_forest", "old_growth_birch_forest");
        putRename("tall_birch_hills", "old_growth_birch_forest");
        putRename("wooded_badlands_plateau", "wooded_badlands");
        putRename("wooded_hills", "forest");
        putRename("wooded_mountains", "windswept_forest");
        putRename("lofty_peaks", "jagged_peaks");
        putRename("snowcapped_peaks", "frozen_peaks");
        putRename("deep_warm_ocean", "warm_ocean");

        // ===== Canonical reverse renames for backporting (many-to-one → pick canonical) =====
        // Lossy: the original variant is lost in some cases
        CAVES_CLIFFS_REVERSE.put("minecraft:badlands", "minecraft:badlands_plateau");
        CAVES_CLIFFS_REVERSE.put("minecraft:old_growth_spruce_taiga", "minecraft:giant_spruce_taiga");
        CAVES_CLIFFS_REVERSE.put("minecraft:old_growth_pine_taiga", "minecraft:giant_tree_taiga");
        CAVES_CLIFFS_REVERSE.put("minecraft:windswept_gravelly_hills", "minecraft:gravelly_mountains");
        CAVES_CLIFFS_REVERSE.put("minecraft:sparse_jungle", "minecraft:jungle_edge");
        CAVES_CLIFFS_REVERSE.put("minecraft:wooded_badlands", "minecraft:wooded_badlands_plateau");
        CAVES_CLIFFS_REVERSE.put("minecraft:windswept_hills", "minecraft:mountains");
        CAVES_CLIFFS_REVERSE.put("minecraft:snowy_plains", "minecraft:snowy_tundra");
        CAVES_CLIFFS_REVERSE.put("minecraft:stony_shore", "minecraft:stone_shore");
        CAVES_CLIFFS_REVERSE.put("minecraft:windswept_savanna", "minecraft:shattered_savanna");
        CAVES_CLIFFS_REVERSE.put("minecraft:old_growth_birch_forest", "minecraft:tall_birch_forest");
        CAVES_CLIFFS_REVERSE.put("minecraft:windswept_forest", "minecraft:wooded_mountains");
        CAVES_CLIFFS_REVERSE.put("minecraft:jagged_peaks", "minecraft:lofty_peaks");
        CAVES_CLIFFS_REVERSE.put("minecraft:frozen_peaks", "minecraft:snowcapped_peaks");
        // These have the same old/new name but multiple old names merged:
        // jungle, desert, taiga, snowy_taiga, bamboo_jungle, birch_forest,
        // dark_forest, mushroom_fields, swamp, forest, warm_ocean
        // For these, the reverse is identity (the base name is the same).
    }

    private static void putBiome(int id, String name) {
        String full = "minecraft:" + name;
        ID_TO_NAME.put(id, full);
        // Only store first occurrence for reverse (some names collide after renames)
        NAME_TO_ID.putIfAbsent(full, id);
    }

    private static void putRename(String oldName, String newName) {
        CAVES_CLIFFS_FORWARD.put("minecraft:" + oldName, "minecraft:" + newName);
    }

    // ===== Public API =====

    /** Convert a numeric biome ID to its string identifier. */
    public static String fromId(int id) {
        return ID_TO_NAME.getOrDefault(id, "minecraft:unknown_" + id);
    }

    /**
     * Convert a biome string back to its numeric ID.
     * Returns -1 if no mapping exists. For backporting to major &lt; 4.
     */
    public static int toId(String biome) {
        Integer id = NAME_TO_ID.get(biome);
        return id != null ? id : -1;
    }

    /**
     * Apply Caves &amp; Cliffs biome renames (forward-porting).
     * For source data with major &lt; 6.
     */
    public static String applyForwardRename(String biome, int sourceMajor) {
        if (sourceMajor < 6) {
            String renamed = CAVES_CLIFFS_FORWARD.get(biome);
            if (renamed != null) return renamed;
        }
        return biome;
    }

    /**
     * Apply reverse Caves &amp; Cliffs biome renames (back-porting).
     * For writing data targeting major &lt; 6.
     *
     * @param report loss report to record lossy reverse renames
     */
    public static String applyReverseRename(String biome, int targetMajor, LossReport report) {
        if (targetMajor < 6) {
            String reversed = CAVES_CLIFFS_REVERSE.get(biome);
            if (reversed != null) {
                if (report != null) {
                    report.warn("biome-rename",
                        "Reversed '" + biome + "' → '" + reversed + "' (lossy: original variant unknown)");
                }
                return reversed;
            }
        }
        return biome;
    }

    /**
     * Ensure a biome identifier has a namespace prefix.
     * Adds {@code minecraft:} if no colon is present.
     */
    public static String ensureNamespaced(String biome) {
        if (biome != null && !biome.contains(":")) {
            return "minecraft:" + biome;
        }
        return biome;
    }
}
