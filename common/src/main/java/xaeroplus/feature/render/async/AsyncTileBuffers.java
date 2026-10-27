package xaeroplus.feature.render.async;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import xaero.lib.client.level.block.BlockTextureColorUtils;
import xaero.lib.platform.ClientOnlyServices;
import xaero.map.MapProcessor;
import xaero.map.biome.BiomeColorCalculator;
import xaero.map.biome.BlockTintProvider;
import xaero.map.cache.BrokenBlockTintCache;
import xaero.map.region.MapRegion;
import xaero.map.region.MapTile;
import xaero.map.region.MapTileChunk;
import xaero.map.region.MapUpdateFastConfig;
import xaero.map.region.texture.LeafRegionTexture;
import xaero.map.world.MapDimension;
import xaero.map.world.MapWorld;
import xaeroplus.XaeroPlus;
import xaeroplus.feature.extensions.AsyncBufferedMapTileChunk;
import xaeroplus.settings.Settings;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;

/**
 * Builds leaf texture pixel buffers ({@code MapTileChunk.updateBuffers}) on worker threads instead of the
 * render thread. The render thread only submits jobs and copies finished pixels into the texture buffer.
 */
public final class AsyncTileBuffers {
    private static final int THREADS = Math.max(1, Math.min(6, Runtime.getRuntime().availableProcessors() / 2));
    private static final ExecutorService POOL = Executors.newFixedThreadPool(THREADS, new WorkerThreadFactory());
    private static final ThreadLocal<WorkerContext> CONTEXT = new ThreadLocal<>();
    private static final Set<Job> LIVE = ConcurrentHashMap.newKeySet();

    // xaerolib's colour caches are not thread-safe, so workers read these copies and fill misses on the render thread.
    private static final Map<BlockState, Integer> TEXTURE_COLORS = new ConcurrentHashMap<>();
    private static final Map<BlockState, Integer> TINT_INDICES = new ConcurrentHashMap<>();
    private static final BrokenBlockTintCache BROKEN_TINTS = new BrokenBlockTintCache(ConcurrentHashMap.newKeySet());

    private AsyncTileBuffers() {}

    /**
     * The shared worker pool, also used for chunk highlight work.
     */
    public static ExecutorService executor() {
        return POOL;
    }

    public static boolean isWorker() {
        return CONTEXT.get() != null;
    }

    /**
     * Render thread, from {@code LeafRegionTexture.preUpload}/{@code postUpload}.
     *
     * @param commit whether a finished job may be applied now (only within the upload budget)
     * @return true if Xaero's synchronous buffer update must be skipped
     */
    public static boolean handle(MapTileChunk chunk, MapProcessor mapProcessor, boolean commit) {
        var async = (AsyncBufferedMapTileChunk) chunk;
        Job job = async.xaeroPlus$job();
        if (!WorldMapScreenState.isOpen()) {
            // Only the map screen uses workers; otherwise (the minimap) Xaero builds buffers itself.
            if (job != null) {
                job.cancelled = true;
                async.xaeroPlus$setJob(null);
            }
            return false;
        }
        if (!WorldMapScreenState.isVisible(chunk.getX() << 6, chunk.getZ() << 6, 64)) {
            // Off screen: drop any queued or running build and leave the chunk flagged until it is visible again.
            if (job != null) {
                job.cancelled = true;
                async.xaeroPlus$setJob(null);
            }
            return true;
        }
        if (job == null) {
            if (async.xaeroPlus$syncFallback()) {
                async.xaeroPlus$setSyncFallback(!commit);
                return false;
            }
            // Regions being written around the player keep Xaero's immediate path; they change every few ticks.
            if (!chunk.getToUpdateBuffers() || mapProcessor.isWritingPaused() || chunk.getInRegion().isBeingWritten()
                || mapProcessor.getWorld() == null) {
                return false;
            }
            job = new Job(chunk, mapProcessor, new MapUpdateFastConfig(mapProcessor), dimension(chunk, mapProcessor), async.xaeroPlus$generation());
            async.xaeroPlus$setJob(job);
            LIVE.add(job);
            POOL.execute(job);
            return true;
        }
        if (!job.done || !commit || mapProcessor.isWritingPaused()) {
            return true;
        }
        async.xaeroPlus$setJob(null);
        if (job.cancelled) {
            return true; // partial; the chunk is still flagged and gets a new job
        } else if (job.failed) {
            async.xaeroPlus$setSyncFallback(true);
        } else if (job.generation == async.xaeroPlus$generation() && chunk.getToUpdateBuffers()) {
            commit(chunk, job);
        }
        // Otherwise the chunk changed meanwhile and is still flagged for an update, so it gets resubmitted.
        return true;
    }

    /**
     * Same dimension {@code MapTileChunk.updateBuffers} uses, including the override in {@code MixinMapTileChunk}.
     */
    private static MapDimension dimension(MapTileChunk chunk, MapProcessor mapProcessor) {
        MapWorld mapWorld = mapProcessor.getMapWorld();
        if (Settings.REGISTRY.writesWhileDimSwitched.get() && mapWorld.isMultiplayer()) {
            return chunk.getInRegion().getDim();
        }
        return mapWorld.getCurrentDimension();
    }

    /**
     * Cancels every queued and running build; their chunks fall back to Xaero's own path.
     */
    static void cancelAll() {
        for (Job job : LIVE) {
            job.cancelled = true;
        }
    }

    /**
     * Mirrors the bookkeeping of {@code MapTileChunk.updateBuffers}, with the pixels already computed.
     */
    private static void commit(MapTileChunk chunk, Job job) {
        LeafRegionTexture texture = chunk.getLeafTexture();
        MapRegion region = chunk.getInRegion();
        texture.resetTimer();
        synchronized (region) {
            texture.setCachePrepared(false);
            texture.setShouldDownloadFromPBO(false);
            region.setAllCachePrepared(false);
        }
        texture.prepareBuffer();
        ByteBuffer buffer = texture.getDirectColorBuffer();
        int[] pixels = job.pixels;
        for (int tile = 0; tile < 16; tile++) {
            if ((job.tileMask & 1 << tile) == 0) continue;
            int offX = (tile >> 2) * 16;
            int offZ = (tile & 3) * 16;
            for (int z = 0; z < 16; z++) {
                int index = (offZ + z) * 64 + offX;
                for (int x = 0; x < 16; x++, index++) {
                    buffer.putInt(index * 4, pixels[index]);
                }
            }
        }
        texture.postBufferUpdate(job.hasLight);
        chunk.setToUpdateBuffers(false);
        texture.setToUpload(true);
    }

    /**
     * Worker thread: same pixel loop as {@code MapTileChunk.updateBuffers}, writing into a private array.
     */
    private static void build(Job job, WorkerContext context) {
        MapTileChunk chunk = job.chunk;
        MapProcessor mapProcessor = job.mapProcessor;
        MapUpdateFastConfig config = job.config;
        Level world = mapProcessor.getWorld();
        if (world == null) {
            throw new IllegalStateException("No world");
        }
        Registry<Block> blockRegistry = mapProcessor.getWorldBlockRegistry();
        Registry<Biome> biomeRegistry = mapProcessor.worldBiomeRegistry;
        Registry<DimensionType> dimensionTypes = mapProcessor.getWorldDimensionTypeRegistry();
        BlockTintProvider tintProvider = context.tintProvider(mapProcessor, biomeRegistry, config.biomeBlending);
        BlockTextureColorUtils textureColors = ClientOnlyServices.PLATFORM.getBlockUtils().getTextureColorUtils();
        LeafRegionTexture texture = chunk.getLeafTexture();
        MapTileChunk prev = chunk.getNeighbourTileChunk(0, -1, mapProcessor, false);
        MapTileChunk prevDiagonal = chunk.getNeighbourTileChunk(-1, -1, mapProcessor, false);
        MapTileChunk prevHorizontal = chunk.getNeighbourTileChunk(-1, 0, mapProcessor, false);
        MapDimension dim = job.dim;
        float shadowR = dim.getShadowR();
        float shadowG = dim.getShadowG();
        float shadowB = dim.getShadowB();
        int[] result = context.result;
        int[] pixels = job.pixels;
        var async = (AsyncBufferedMapTileChunk) chunk;

        for (int o = 0; o < 4; o++) {
            for (int p = 0; p < 4; p++) {
                if (job.cancelled || async.xaeroPlus$generation() != job.generation) {
                    return; // discarded; avoid touching tiles that may already be recycled
                }
                MapTile tile = chunk.getTile(o, p);
                if (tile == null || !tile.isLoaded()) continue;
                int caveStart = tile.getWrittenCaveStart();
                int caveDepth = tile.getWrittenCaveDepth();
                for (int z = 0; z < 16; z++) {
                    int pixelZ = p * 16 + z;
                    for (int x = 0; x < 16; x++) {
                        int pixelX = o * 16 + x;
                        tile.getBlock(x, z).getPixelColour(
                            result, textureColors, world, dim, blockRegistry, chunk, prev, prevDiagonal, prevHorizontal, tile, x, z,
                            caveStart, caveDepth, context.pos, biomeRegistry, dimensionTypes, shadowR, shadowG, shadowB,
                            tintProvider, mapProcessor, mapProcessor.getOverlayManager(),
                            texture.getHeight(pixelX, pixelZ), texture.getTopHeight(pixelX, pixelZ),
                            mapProcessor.getBlockStateShortShapeCache(), config);
                        pixels[pixelZ * 64 + pixelX] = result[2] << 24 | result[1] << 16 | result[0] << 8 | result[3];
                        if (result[3] != 0) {
                            job.hasLight = true;
                        }
                    }
                }
                job.tileMask |= 1 << (o * 4 + p);
            }
        }
    }

    /**
     * Worker replacement for {@code BlockTextureColorUtils.getBlockTextureColor}.
     */
    public static int textureColor(BlockTextureColorUtils utils, BlockState state, IntSupplier compute) {
        Integer color = TEXTURE_COLORS.get(state);
        if (color != null) return color;
        return onRenderThread(() -> {
            int computed = compute.getAsInt();
            TEXTURE_COLORS.put(state, computed);
            TINT_INDICES.put(state, utils.getBlockTintIndex(state));
            return computed;
        });
    }

    /**
     * Worker replacement for {@code BlockTextureColorUtils.getBlockTintIndex}.
     */
    public static int tintIndex(BlockState state, IntSupplier compute) {
        Integer tint = TINT_INDICES.get(state);
        if (tint != null) return tint;
        return onRenderThread(() -> {
            int computed = compute.getAsInt();
            TINT_INDICES.putIfAbsent(state, computed);
            return computed;
        });
    }

    public static void clearColorCaches() {
        TEXTURE_COLORS.clear();
        TINT_INDICES.clear();
    }

    private static int onRenderThread(IntSupplier task) {
        try {
            return Minecraft.getInstance().submit(task::getAsInt).get(10, TimeUnit.SECONDS);
        } catch (final Exception e) {
            throw new IllegalStateException("Render thread colour lookup failed", e);
        }
    }

    public static final class Job implements Runnable {
        final MapTileChunk chunk;
        final MapProcessor mapProcessor;
        final MapUpdateFastConfig config;
        final MapDimension dim;
        final int generation;
        int[] pixels;
        int tileMask;
        boolean hasLight;
        boolean failed;
        volatile boolean cancelled;
        volatile boolean done;

        Job(MapTileChunk chunk, MapProcessor mapProcessor, MapUpdateFastConfig config, MapDimension dim, int generation) {
            this.chunk = chunk;
            this.mapProcessor = mapProcessor;
            this.config = config;
            this.dim = dim;
            this.generation = generation;
        }

        @Override
        public void run() {
            if (cancelled) {
                LIVE.remove(this);
                done = true;
                return;
            }
            MapRegion region = chunk.getInRegion();
            // Keeps MapWriter off this region's tiles while they are being read.
            region.pushWriterPause();
            try {
                pixels = new int[64 * 64];
                build(this, CONTEXT.get());
            } catch (final Throwable t) {
                failed = true;
                XaeroPlus.LOGGER.debug("Async buffer build failed for chunk {}", chunk, t);
            } finally {
                region.popWriterPause();
                LIVE.remove(this);
                done = true;
            }
        }
    }

    private static final class WorkerContext {
        final int[] result = new int[4];
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        final BiomeColorCalculator calculator = new BiomeColorCalculator();
        BlockTintProvider source;
        BlockTintProvider tintProvider;

        /**
         * A private tint provider per worker, rebuilt whenever Xaero replaces its own (world change).
         */
        BlockTintProvider tintProvider(MapProcessor mapProcessor, Registry<Biome> biomeRegistry, boolean biomeBlending) {
            BlockTintProvider current = mapProcessor.getWorldBlockTintProvider();
            if (current != source || tintProvider == null) {
                source = current;
                tintProvider = new BlockTintProvider(biomeRegistry, calculator, mapProcessor, BROKEN_TINTS, mapProcessor.getMapWriter());
            }
            calculator.prepare(biomeBlending);
            return tintProvider;
        }
    }

    private static final class WorkerThreadFactory implements java.util.concurrent.ThreadFactory {
        private final AtomicInteger count = new AtomicInteger();

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(() -> {
                CONTEXT.set(new WorkerContext());
                runnable.run();
            }, "XaeroPlus WorldMap Buffer Worker #" + count.incrementAndGet());
            thread.setDaemon(true);
            thread.setPriority(Thread.NORM_PRIORITY - 2);
            return thread;
        }
    }
}
