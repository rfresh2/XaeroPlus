package xaeroplus.util.normalizer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads {@code vanilla_states.dat} — the static bundled resource mapping legacy
 * pre-1.13 numeric block state IDs to their NBT representations.
 *
 * <p>File format: sequence of {@code (int stateId, CompoundTag nbt)} pairs.
 * The int packs {@code blockId = stateId & 0xFFF}, {@code meta = (stateId >> 12) & 0xFFFFF}.</p>
 *
 * <p>Provides both forward (ID → tag) and reverse (tag string → ID) lookups.</p>
 */
public final class VanillaStatesLoader {
    private VanillaStatesLoader() {}

    /**
     * Load vanilla_states.dat from the given input stream.
     *
     * @param inputStream stream of the vanilla_states.dat resource
     * @return map from stateId to block state CompoundTag
     */
    public static Map<Integer, CompoundTag> load(InputStream inputStream) throws IOException {
        Map<Integer, CompoundTag> map = new HashMap<>();
        try (DataInputStream dis = new DataInputStream(inputStream)) {
            while (dis.available() > 0) {
                int stateId = dis.readInt();
                CompoundTag tag = NbtIo.read(dis);
                map.put(stateId, tag);
            }
        }
        return map;
    }

    /**
     * Try to load vanilla_states.dat from the classpath.
     * Falls back to an empty map if not found.
     */
    public static Map<Integer, CompoundTag> loadFromClasspath() {
        try (InputStream is = VanillaStatesLoader.class.getResourceAsStream(
                "/assets/xaeroworldmap/vanilla_states.dat")) {
            if (is != null) return load(is);
        } catch (IOException e) {
            // fall through
        }
        return new HashMap<>();
    }

    /**
     * Build a reverse lookup from state string → numeric stateId.
     * Used when backporting to major version 0 (pre-1.13).
     *
     * @param forwardMap the ID → CompoundTag map from {@link #load} or {@link #loadFromClasspath}
     * @return map from canonical state string to stateId
     */
    public static Map<String, Integer> buildReverseLookup(Map<Integer, CompoundTag> forwardMap) {
        Map<String, Integer> reverse = new HashMap<>();
        for (Map.Entry<Integer, CompoundTag> entry : forwardMap.entrySet()) {
            String stateStr = BlockStateFixers.tagToString(entry.getValue());
            // First occurrence wins (prefer lower stateId / default meta)
            reverse.putIfAbsent(stateStr, entry.getKey());
        }
        return reverse;
    }
}
