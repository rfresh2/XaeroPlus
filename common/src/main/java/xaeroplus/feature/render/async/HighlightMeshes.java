package xaeroplus.feature.render.async;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.util.ARGB;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import xaeroplus.XaeroPlus;
import xaeroplus.feature.render.highlight.MultiColorHighlightColorFunction;
import xaeroplus.util.ChunkUtils;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.CompletableFuture;

/**
 * Builds chunk highlight meshes on worker threads, sampled down to about one quad per screen pixel.
 * Builds stop and free their memory when the map screen closes.
 */
public final class HighlightMeshes {
    private static final VarHandle KEY;
    private static final VarHandle VALUE;
    private static final VarHandle CONTAINS_NULL_KEY;

    static {
        try {
            var lookup = MethodHandles.privateLookupIn(Long2LongOpenHashMap.class, MethodHandles.lookup());
            KEY = lookup.findVarHandle(Long2LongOpenHashMap.class, "key", long[].class);
            VALUE = lookup.findVarHandle(Long2LongOpenHashMap.class, "value", long[].class);
            CONTAINS_NULL_KEY = lookup.findVarHandle(Long2LongOpenHashMap.class, "containsNullKey", boolean.class);
        } catch (final ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private HighlightMeshes() {}

    /**
     * Each cell spans 2^shift chunks per side, sized so that a cell is at most one framebuffer pixel.
     */
    public static int cellShift(double pixelsPerBlock) {
        double chunksPerPixel = 1.0 / (16.0 * pixelsPerBlock);
        return chunksPerPixel < 2.0 ? 0 : Math.min(20, 31 - Integer.numberOfLeadingZeros((int) Math.min(chunksPerPixel, 1 << 20)));
    }

    /**
     * Render thread: captures the map, then builds the mesh on a worker.
     */
    public static CompletableFuture<Mesh> build(Long2LongMap highlights, boolean flipped, int cellShift,
                                                @Nullable MultiColorHighlightColorFunction colorFunction) {
        Snapshot snapshot = Snapshot.of(highlights);
        int session = WorldMapScreenState.session();
        return CompletableFuture.supplyAsync(() -> {
            try {
                return sampled(snapshot, flipped, cellShift, colorFunction, session);
            } catch (final Throwable t) {
                XaeroPlus.LOGGER.warn("Failed to build chunk highlight mesh", t);
                return null;
            }
        }, AsyncTileBuffers.executor());
    }

    /**
     * Point-samples the highlights like the GPU does when one quad per chunk is drawn at sub-pixel size: each
     * 2^shift cell shows the chunk at its centre, in that chunk's colour, or nothing. Sparse highlights stay sparse.
     * At shift 0 this is exactly the regular mesh, one quad per highlighted chunk.
     */
    private static @Nullable Mesh sampled(Snapshot snapshot, boolean flipped, int shift,
                                          @Nullable MultiColorHighlightColorFunction colorFunction, int session) {
        int mask = (1 << shift) - 1;
        int centre = mask + 1 >> 1;
        int[] count = new int[1];
        boolean complete = snapshot.forEach(session, (pos, value) -> {
            if ((ChunkUtils.longToChunkX(pos) & mask) == centre && (ChunkUtils.longToChunkZ(pos) & mask) == centre) {
                count[0]++;
            }
        });
        if (!complete) return null;
        Mesh mesh = new Mesh(count[0], colorFunction != null);
        complete = snapshot.forEach(session, (pos, value) -> {
            int x = ChunkUtils.longToChunkX(pos);
            int z = ChunkUtils.longToChunkZ(pos);
            if ((x & mask) != centre || (z & mask) != centre) return;
            int color = 0;
            if (colorFunction != null) {
                color = colorFunction.getColor(pos, value);
                if (color >>> 24 == 0) return;
            }
            mesh.quad(x - centre, z - centre, mask + 1, flipped, color);
        });
        if (!complete) {
            mesh.free();
            return null;
        }
        return mesh;
    }

    /**
     * Worker thread: keys of all entries outside the given chunk bounds.
     */
    public static LongArrayList keysOutside(Snapshot snapshot, int minX, int maxX, int minZ, int maxZ) {
        LongArrayList outside = new LongArrayList();
        snapshot.forEach(-1, (pos, value) -> {
            int x = ChunkUtils.longToChunkX(pos);
            int z = ChunkUtils.longToChunkZ(pos);
            if (x < minX || x > maxX || z < minZ || z > maxZ) {
                outside.add(pos);
            }
        });
        return outside;
    }

    @FunctionalInterface
    interface EntryConsumer {
        void accept(long key, long value);
    }

    /**
     * Read-only view of a highlight map for another thread. For open hash maps it holds the backing
     * arrays rather than a copy, so the render thread pays nothing. The owner may keep writing to them meanwhile, and a
     * rehash swaps in new arrays, so a reader can at worst miss or repeat an entry. Callers only use it for the
     * rendered mesh, which the next refresh replaces, and for picking removal candidates, which are re-checked
     * against the live map on the render thread.
     */
    public record Snapshot(long[] keys, long[] values, boolean containsNullKey) {
        public static Snapshot of(Long2LongMap map) {
            var open = map instanceof Long2LongOpenHashMap hashMap ? hashMap : new Long2LongOpenHashMap(map);
            return new Snapshot((long[]) KEY.get(open), (long[]) VALUE.get(open), (boolean) CONTAINS_NULL_KEY.get(open));
        }

        /**
         * @param session the map screen session to stop at the end of, or -1 to always finish
         * @return false if stopped early
         */
        boolean forEach(int session, EntryConsumer consumer) {
            int n = Math.min(keys.length, values.length) - 1;
            for (int i = 0; i < n; i++) {
                if ((i & 0xFFFF) == 0 && session != -1 && WorldMapScreenState.session() != session) {
                    return false;
                }
                long key = keys[i];
                if (key != 0) {
                    consumer.accept(key, values[i]);
                }
            }
            if (containsNullKey) {
                consumer.accept(0, values[n]);
            }
            return true;
        }
    }

    /**
     * Quads in the highlight vertex layout: POSITION, or POSITION_COLOR when multi-coloured.
     */
    public static final class Mesh {
        private final boolean colored;
        private final int capacity;
        private ByteBuffer data;
        private int quads;

        Mesh(int capacity, boolean colored) {
            this.colored = colored;
            this.capacity = capacity;
            this.data = MemoryUtil.memAlloc(Math.max(1, capacity) * 4 * (colored ? 16 : 12));
        }

        void quad(int x, int z, int size, boolean flipped, int argb) {
            if (quads == capacity) {
                return; // the map grew while it was being read; the next refresh picks the rest up
            }
            quads++;
            float x1 = x;
            float x2 = x + size;
            float y1 = flipped ? z + size : z;
            float y2 = flipped ? z : z + size;
            int abgr = ARGB.toABGR(argb);
            vertex(x1, y2, abgr);
            vertex(x2, y2, abgr);
            vertex(x2, y1, abgr);
            vertex(x1, y1, abgr);
        }

        private void vertex(float x, float y, int abgr) {
            data.putFloat(x).putFloat(y).putFloat(0.0F);
            if (colored) {
                // BufferBuilder always stores colour bytes as R, G, B, A.
                data.putInt(data.order() == ByteOrder.LITTLE_ENDIAN ? abgr : Integer.reverseBytes(abgr));
            }
        }

        public int vertexCount() {
            return quads * 4;
        }

        public ByteBuffer data() {
            return data.flip();
        }

        public void free() {
            if (data != null) {
                MemoryUtil.memFree(data);
                data = null;
            }
        }
    }
}
