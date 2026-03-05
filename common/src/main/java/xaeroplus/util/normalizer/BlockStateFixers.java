package xaeroplus.util.normalizer;

import net.minecraft.nbt.CompoundTag;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Block state fixer pipeline — forward (upgrade) and reverse (downgrade).
 *
 * <p>Forward fixers upgrade old block state representations to modern equivalents.
 * Reverse fixers downgrade modern representations for backporting.
 * Both operate on CompoundTag with structure {@code {Name: "namespace:id", Properties: {...}}}.</p>
 */
public final class BlockStateFixers {
    private BlockStateFixers() {}

    // ===== Version 1 (1.13→1.14) block renames =====
    private static final Map<String, String> V1_FORWARD = Map.of(
        "minecraft:stone_slab", "minecraft:smooth_stone_slab",
        "minecraft:sign", "minecraft:oak_sign",
        "minecraft:wall_sign", "minecraft:oak_wall_sign"
    );
    private static final Map<String, String> V1_REVERSE = Map.of(
        "minecraft:smooth_stone_slab", "minecraft:stone_slab",
        "minecraft:oak_sign", "minecraft:sign",
        "minecraft:oak_wall_sign", "minecraft:wall_sign"
    );

    // ===== Jigsaw facing → orientation (version < 3) =====
    private static final Map<String, String> JIGSAW_FACING_TO_ORIENT = Map.of(
        "", "north_up",
        "down", "down_south",
        "up", "up_north",
        "north", "north_up",
        "south", "south_up",
        "west", "west_up",
        "east", "east_up"
    );
    private static final Map<String, String> JIGSAW_ORIENT_TO_FACING = Map.of(
        "north_up", "north",
        "down_south", "down",
        "up_north", "up",
        "south_up", "south",
        "west_up", "west",
        "east_up", "east"
    );

    // ===== Wall blocks (version < 3) =====
    private static final Set<String> WALL_BLOCKS = Set.of(
        "minecraft:andesite_wall", "minecraft:brick_wall",
        "minecraft:cobblestone_wall", "minecraft:diorite_wall",
        "minecraft:end_stone_brick_wall", "minecraft:granite_wall",
        "minecraft:mossy_cobblestone_wall", "minecraft:mossy_stone_brick_wall",
        "minecraft:nether_brick_wall", "minecraft:prismarine_wall",
        "minecraft:red_nether_brick_wall", "minecraft:red_sandstone_wall",
        "minecraft:sandstone_wall", "minecraft:stone_brick_wall"
    );
    private static final Set<String> WALL_DIRS = Set.of("east", "west", "north", "south");

    // ===================================================================
    //  FORWARD FIXERS (upgrade old → modern)
    // ===================================================================

    /**
     * Apply all forward fixers based on source version.
     * Modifies the tag in-place and returns it.
     */
    public static CompoundTag applyForward(CompoundTag tag, int sourceMajor) {
        if (sourceMajor < 2) fwdV1Renames(tag);
        if (sourceMajor < 3) fwdV3Fixes(tag);
        if (sourceMajor < 5) fwdV5Fixes(tag);
        if (sourceMajor < 7) fwdV7Fixes(tag);
        return tag;
    }

    // --- V1: Block renames ---
    private static void fwdV1Renames(CompoundTag tag) {
        String name = tag.getString("Name");
        String renamed = V1_FORWARD.get(name);
        if (renamed != null) tag.putString("Name", renamed);
    }

    // --- V3: Jigsaw, redstone wire, walls ---
    private static void fwdV3Fixes(CompoundTag tag) {
        String name = tag.getString("Name");
        if ("minecraft:jigsaw".equals(name)) {
            CompoundTag props = getOrCreateProps(tag);
            String facing = props.getString("facing");
            props.remove("facing");
            props.putString("orientation", JIGSAW_FACING_TO_ORIENT.getOrDefault(facing, "north_up"));
        } else if ("minecraft:redstone_wire".equals(name)) {
            fwdRedstoneWire(tag);
        } else if (WALL_BLOCKS.contains(name)) {
            fwdWall(tag);
        }
    }

    private static void fwdRedstoneWire(CompoundTag tag) {
        CompoundTag props = getOrCreateProps(tag);
        for (String dir : List.of("north", "south", "east", "west")) {
            if (props.getString(dir).isEmpty()) {
                props.putString(dir, "none");
            }
        }
        // Cross logic: if all 4 horizontal sides are "none" → set all to "side"
        boolean allNone = true;
        for (String dir : List.of("north", "south", "east", "west")) {
            if (!"none".equals(props.getString(dir))) { allNone = false; break; }
        }
        if (allNone) {
            for (String dir : List.of("north", "south", "east", "west")) {
                props.putString(dir, "side");
            }
        }
    }

    private static void fwdWall(CompoundTag tag) {
        CompoundTag props = getOrCreateProps(tag);
        for (String dir : WALL_DIRS) {
            String val = props.getString(dir);
            if ("true".equals(val)) props.putString(dir, "low");
            else if ("false".equals(val)) props.putString(dir, "none");
        }
    }

    // --- V5: Cauldron, grass path ---
    private static void fwdV5Fixes(CompoundTag tag) {
        String name = tag.getString("Name");
        if ("minecraft:cauldron".equals(name)) {
            if (tag.contains("Properties")) {
                CompoundTag props = tag.getCompound("Properties");
                String level = props.getString("level");
                if (!level.isEmpty() && !"0".equals(level)) {
                    tag.putString("Name", "minecraft:water_cauldron");
                } else {
                    tag.remove("Properties");
                }
            }
        } else if ("minecraft:grass_path".equals(name)) {
            tag.putString("Name", "minecraft:dirt_path");
        }
    }

    // --- V7: Creaking heart ---
    private static void fwdV7Fixes(CompoundTag tag) {
        String name = tag.getString("Name");
        if ("minecraft:creaking_heart".equals(name)) {
            CompoundTag props = getOrCreateProps(tag);
            String active = props.getString("active");
            props.remove("active");
            props.putString("creaking_heart_state", "true".equals(active) ? "awake" : "uprooted");
        }
    }

    // ===================================================================
    //  REVERSE FIXERS (downgrade modern → old for backporting)
    // ===================================================================

    /**
     * Apply reverse fixers to downgrade a modern block state for writing at the given target version.
     *
     * @param tag           block state CompoundTag
     * @param targetMajor   target format major version
     * @param report        loss report to record lossy operations (may be null)
     * @return the modified tag
     */
    public static CompoundTag applyReverse(CompoundTag tag, int targetMajor, LossReport report) {
        // Apply in reverse order (highest version boundary first)
        if (targetMajor < 7) revV7Fixes(tag);
        if (targetMajor < 5) revV5Fixes(tag);
        if (targetMajor < 3) revV3Fixes(tag, report);
        if (targetMajor < 2) revV1Renames(tag);
        return tag;
    }

    // --- Reverse V1 ---
    private static void revV1Renames(CompoundTag tag) {
        String name = tag.getString("Name");
        String reversed = V1_REVERSE.get(name);
        if (reversed != null) tag.putString("Name", reversed);
    }

    // --- Reverse V3 ---
    private static void revV3Fixes(CompoundTag tag, LossReport report) {
        String name = tag.getString("Name");
        if ("minecraft:jigsaw".equals(name)) {
            CompoundTag props = getOrCreateProps(tag);
            String orient = props.getString("orientation");
            props.remove("orientation");
            props.putString("facing", JIGSAW_ORIENT_TO_FACING.getOrDefault(orient, "north"));
        } else if ("minecraft:redstone_wire".equals(name)) {
            revRedstoneWire(tag);
        } else if (WALL_BLOCKS.contains(name)) {
            revWall(tag, report);
        }
    }

    private static void revRedstoneWire(CompoundTag tag) {
        CompoundTag props = getOrCreateProps(tag);
        for (String dir : List.of("north", "south", "east", "west")) {
            String val = props.getString(dir);
            if ("none".equals(val) || "side".equals(val)) {
                props.putString(dir, "");
            }
        }
    }

    private static void revWall(CompoundTag tag, LossReport report) {
        CompoundTag props = getOrCreateProps(tag);
        boolean lostTall = false;
        for (String dir : WALL_DIRS) {
            String val = props.getString(dir);
            if ("low".equals(val)) {
                props.putString(dir, "true");
            } else if ("tall".equals(val)) {
                props.putString(dir, "true");
                lostTall = true;
            } else if ("none".equals(val)) {
                props.putString(dir, "false");
            }
        }
        if (lostTall && report != null) {
            report.warn("block-fixer",
                "Wall '" + tag.getString("Name") + "': tall/low distinction → true (lossy)");
        }
    }

    // --- Reverse V5 ---
    private static void revV5Fixes(CompoundTag tag) {
        String name = tag.getString("Name");
        if ("minecraft:water_cauldron".equals(name)) {
            tag.putString("Name", "minecraft:cauldron");
            // Properties (including level) are preserved
        } else if ("minecraft:dirt_path".equals(name)) {
            tag.putString("Name", "minecraft:grass_path");
        }
    }

    // --- Reverse V7 ---
    private static void revV7Fixes(CompoundTag tag) {
        String name = tag.getString("Name");
        if ("minecraft:creaking_heart".equals(name)) {
            CompoundTag props = getOrCreateProps(tag);
            String state = props.getString("creaking_heart_state");
            props.remove("creaking_heart_state");
            props.putString("active", "awake".equals(state) ? "true" : "false");
        }
    }

    // ===================================================================
    //  State string ↔ CompoundTag conversion
    // ===================================================================

    /**
     * Convert a CompoundTag block state to its canonical string representation.
     * Format: {@code namespace:id} or {@code namespace:id[prop1=val1,prop2=val2]}
     * Properties are sorted alphabetically.
     */
    public static String tagToString(CompoundTag tag) {
        String name = tag.getString("Name");
        if (name.isEmpty()) return "minecraft:unknown";
        if (tag.contains("Properties")) {
            CompoundTag props = tag.getCompound("Properties");
            if (!props.isEmpty()) {
                String propsStr = props.getAllKeys().stream()
                    .sorted()
                    .map(k -> k + "=" + props.getString(k))
                    .collect(Collectors.joining(",", "[", "]"));
                return name + propsStr;
            }
        }
        return name;
    }

    /**
     * Parse a state string back into a CompoundTag.
     * Input: {@code "minecraft:stone"} or {@code "minecraft:stone[variant=granite,polished=true]"}
     */
    public static CompoundTag stringToTag(String stateString) {
        CompoundTag tag = new CompoundTag();
        int bracketStart = stateString.indexOf('[');
        if (bracketStart == -1) {
            tag.putString("Name", stateString);
        } else {
            tag.putString("Name", stateString.substring(0, bracketStart));
            String propsStr = stateString.substring(bracketStart + 1, stateString.length() - 1);
            if (!propsStr.isEmpty()) {
                CompoundTag props = new CompoundTag();
                for (String pair : propsStr.split(",")) {
                    int eq = pair.indexOf('=');
                    if (eq > 0) {
                        props.putString(pair.substring(0, eq), pair.substring(eq + 1));
                    }
                }
                tag.put("Properties", props);
            }
        }
        return tag;
    }

    // ===== Helpers =====

    private static CompoundTag getOrCreateProps(CompoundTag tag) {
        if (!tag.contains("Properties")) {
            CompoundTag props = new CompoundTag();
            tag.put("Properties", props);
            return props;
        }
        return tag.getCompound("Properties");
    }
}
