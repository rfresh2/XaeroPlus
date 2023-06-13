package xaeroplus.mixin;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.Registry;
import net.minecraft.state.State;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.MapProcessor;
import xaero.map.MapWriter;
import xaero.map.WorldMap;
import xaero.map.biome.BiomeColorCalculator;
import xaero.map.biome.BiomeGetter;
import xaero.map.biome.BlockTintProvider;
import xaero.map.cache.BlockStateShortShapeCache;
import xaero.map.core.XaeroWorldMapCore;
import xaero.map.misc.CachedFunction;
import xaero.map.misc.Misc;
import xaero.map.region.*;
import xaeroplus.settings.XaeroPlusSettingRegistry;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.nonNull;

@Mixin(value = MapWriter.class, remap = false)
public abstract class MixinMapWriter {
    // insert our own limiter on new tiles being written but this one's keyed on the actual chunk
    // tile "writes" also include a lot of extra operations and lookups before any writing is actually done
    // when we remove existing limiters those extra operations add up to a lot of unnecessary cpu time
    private final Cache<String, Instant> tileUpdateCache = Caffeine.newBuilder()
            // I would usually expect even second long expiration here to be fine
            // but there are some operations that make repeat invocations actually required
            // perhaps another time ill rewrite those. Or make the cache lock more aware of when we don't have any new updates to write/load
            // there's still alot of performance and efficiency on the table to regain
            // but i think this is a good middle ground for now
            .maximumSize(1000)
            .expireAfterWrite(5L, TimeUnit.SECONDS)
            .<String, Instant>build();
    public long writeFreeSinceLastWrite = -1L;
    @Shadow
    private int X;
    @Shadow
    private int Z;
    @Shadow
    private int playerChunkX;
    @Shadow
    private int playerChunkZ;
    @Shadow
    private int loadDistance;
    @Shadow
    private int startTileChunkX;
    @Shadow
    private int startTileChunkZ;
    @Shadow
    private int endTileChunkX;
    @Shadow
    private int endTileChunkZ;
    @Shadow
    private int insideX;
    @Shadow
    private int insideZ;
    @Shadow
    private long updateCounter;
    @Shadow
    private int caveStart;
    @Shadow
    private int writingLayer = Integer.MAX_VALUE;
    @Shadow
    private int writtenCaveStart = Integer.MAX_VALUE;
    @Shadow
    private boolean clearCachedColours;
    @Shadow
    private MapBlock loadingObject;
    @Shadow
    private OverlayBuilder overlayBuilder;
    @Final
    @Shadow
    private BlockPos.Mutable mutableLocalPos;
    @Final
    @Shadow
    private BlockPos.Mutable mutableGlobalPos;
    @Shadow
    private Random usedRandom = Random.create(0L);
    @Shadow
    private long lastWrite = -1L;
    @Shadow
    private long lastWriteTry = -1L;
    @Shadow
    private int workingFrameCount;
    @Shadow
    private long framesFreedTime = -1L;
    @Shadow
    private int writeFreeSizeTiles;
    @Shadow
    private int writeFreeFullUpdateTargetTime;
    @Shadow
    private MapProcessor mapProcessor;
    @Shadow
    private ArrayList<BlockState> buggedStates;
    @Shadow
    private BlockStateShortShapeCache blockStateShortShapeCache;
    @Shadow
    private int topH;
    @Final
    @Shadow
    private CachedFunction<State<?, ?>, Boolean> transparentCache;
    @Shadow
    private int firstTransparentStateY;
    @Final
    @Shadow
    private BlockPos.Mutable mutableBlockPos3;
    @Shadow
    private CachedFunction<FluidState, BlockState> fluidToBlock;
    @Shadow
    private BiomeGetter biomeGetter;
    @Shadow
    private ArrayList<MapRegion> regionBuffer = new ArrayList<>();
    @Shadow
    private HashMap<String, Integer> textureColours;
    @Shadow
    private HashMap<BlockState, Integer> blockColours;
    @Shadow
    private long lastLayerSwitch;
    @Shadow
    private BlockState lastBlockStateForTextureColor = null;
    @Shadow
    private int lastBlockStateForTextureColorResult = -1;

    @Shadow
    public abstract boolean writeMap(
            World world,
            Registry<Block> blockRegistry,
            double playerX,
            double playerY,
            double playerZ,
            Registry<Biome> biomeRegistry,
            BiomeColorCalculator biomeColorCalculator,
            OverlayManager overlayManager,
            boolean loadChunks,
            boolean updateChunks,
            boolean ignoreHeightmaps,
            boolean flowers,
            boolean detailedDebug,
            BlockPos.Mutable mutableBlockPos3,
            BlockTintProvider blockTintProvider,
            int caveDepth
    );

    @Shadow
    protected abstract boolean shouldOverlayCached(State<?, ?> state);

    @Shadow
    public abstract boolean hasVanillaColor(BlockState state, World world, Registry<Block> blockRegistry, BlockPos pos);

    @Shadow
    public abstract boolean isInvisible(World world, BlockState state, Block b, boolean flowers);

    @Shadow
    public abstract boolean isGlowing(BlockState state);

    @Shadow
    protected abstract BlockState unpackFramedBlocks(BlockState original, World world, BlockPos globalPos);

    /**
     * @author rfresh2
     * @reason remove limiters on map write frequency
     */
    @Overwrite
    public void onRender(BiomeColorCalculator biomeColorCalculator, OverlayManager overlayManager) {
        long before = System.nanoTime();

        try {
            if (WorldMap.crashHandler.getCrashedBy() == null) {
                synchronized (this.mapProcessor.renderThreadPauseSync) {
                    if (!this.mapProcessor.isWritingPaused()
                            && !this.mapProcessor.isWaitingForWorldUpdate()
                            && this.mapProcessor.getMapSaveLoad().isRegionDetectionComplete()
                            && this.mapProcessor.isCurrentMultiworldWritable()) {
                        if (this.mapProcessor.getWorld() == null || this.mapProcessor.isCurrentMapLocked()) {
                            return;
                        }

                        if (this.mapProcessor.getCurrentWorldId() != null
                                && !this.mapProcessor.ignoreWorld(this.mapProcessor.getWorld())
                                && (WorldMap.settings.updateChunks || WorldMap.settings.loadChunks || !this.mapProcessor.getMapWorld().isMultiplayer())) {
                            double playerX;
                            double playerY;
                            double playerZ;
                            synchronized (this.mapProcessor.mainStuffSync) {
                                if (this.mapProcessor.mainWorld != this.mapProcessor.getWorld()) {
                                    return;
                                }

                                playerX = this.mapProcessor.mainPlayerX;
                                playerY = this.mapProcessor.mainPlayerY;
                                playerZ = this.mapProcessor.mainPlayerZ;
                            }

                            XaeroWorldMapCore.ensureField();
                            int lengthX = this.endTileChunkX - this.startTileChunkX + 1;
                            int lengthZ = this.endTileChunkZ - this.startTileChunkZ + 1;
                            if (this.lastWriteTry == -1L) {
                                lengthX = 3;
                                lengthZ = 3;
                            }

                            int sizeTileChunks = lengthX * lengthZ;
                            int sizeTiles = sizeTileChunks * 4 * 4;
                            int sizeBasedTargetTime = sizeTiles * 1000 / 1500;
                            int fullUpdateTargetTime = Math.max(100, sizeBasedTargetTime);
                            long time = System.currentTimeMillis();
                            long passed = this.lastWrite == -1L ? 0L : time - this.lastWrite;
                            if (this.lastWriteTry == -1L
                                    || this.writeFreeSizeTiles != sizeTiles
                                    || this.writeFreeFullUpdateTargetTime != fullUpdateTargetTime
                                    || this.workingFrameCount > 30) {
                                this.framesFreedTime = time;
                                this.writeFreeSizeTiles = sizeTiles;
                                this.writeFreeFullUpdateTargetTime = fullUpdateTargetTime;
                                this.workingFrameCount = 0;
                            }
                            long sinceLastWrite;
                            if (this.framesFreedTime != -1L) {
                                sinceLastWrite = time - this.framesFreedTime;
                            } else {
                                sinceLastWrite = Math.min(passed, this.writeFreeSinceLastWrite);
                            }
                            sinceLastWrite = Math.max(1L, sinceLastWrite);

                            long tilesToUpdate = XaeroPlusSettingRegistry.fastMapSetting.getValue()
                                    ? (long) Math.min(sizeTiles, XaeroPlusSettingRegistry.fastMapMaxTilesPerCycle.getValue())
                                    : Math.min(sinceLastWrite * (long) sizeTiles / (long) fullUpdateTargetTime, 100L); // default

                            if (this.lastWrite == -1L || tilesToUpdate != 0L) {
                                this.lastWrite = time;
                            }

                            if (tilesToUpdate != 0L) {
                                if (this.framesFreedTime != -1L) {
                                    this.writeFreeSinceLastWrite = sinceLastWrite;
                                    this.framesFreedTime = -1L;
                                } else {
                                    int timeLimit = (int) (Math.min(sinceLastWrite, 50L) * 86960L);
                                    long writeStartNano = System.nanoTime();
                                    Registry<Biome> biomeRegistry = this.mapProcessor.worldBiomeRegistry;
                                    boolean loadChunks = WorldMap.settings.loadChunks || !this.mapProcessor.getMapWorld().isMultiplayer();
                                    boolean updateChunks = WorldMap.settings.updateChunks || !this.mapProcessor.getMapWorld().isMultiplayer();
                                    boolean ignoreHeightmaps = this.mapProcessor.getMapWorld().isIgnoreHeightmaps();
                                    boolean flowers = WorldMap.settings.flowers;
                                    boolean detailedDebug = WorldMap.settings.detailed_debug;
                                    int caveDepth = WorldMap.settings.caveModeDepth;
                                    BlockPos.Mutable mutableBlockPos3 = this.mutableBlockPos3;
                                    BlockTintProvider blockTintProvider = this.mapProcessor.getWorldBlockTintProvider();
                                    ClientWorld world = this.mapProcessor.getWorld();
                                    Registry<Block> blockRegistry = this.mapProcessor.getWorldBlockRegistry();

                                    for (int i = 0; (long) i < tilesToUpdate; ++i) {
                                        if (this.writeMap(
                                                world,
                                                blockRegistry,
                                                playerX,
                                                playerY,
                                                playerZ,
                                                biomeRegistry,
                                                biomeColorCalculator,
                                                overlayManager,
                                                loadChunks,
                                                updateChunks,
                                                ignoreHeightmaps,
                                                flowers,
                                                detailedDebug,
                                                mutableBlockPos3,
                                                blockTintProvider,
                                                caveDepth
                                        )) {
                                            --i;
                                        }

                                        /** removing time limit **/
                                        if (!XaeroPlusSettingRegistry.fastMapSetting.getValue()) {
                                            if (System.nanoTime() - writeStartNano >= (long) timeLimit) {
                                                break;
                                            }
                                        }
                                    }
                                    ++this.workingFrameCount;
                                }
                            }

                            this.lastWriteTry = time;
                            int startRegionX = this.startTileChunkX >> 3;
                            int startRegionZ = this.startTileChunkZ >> 3;
                            int endRegionX = this.endTileChunkX >> 3;
                            int endRegionZ = this.endTileChunkZ >> 3;
                            boolean shouldRequestLoading = false;
                            LeveledRegion<?> nextToLoad = this.mapProcessor.getMapSaveLoad().getNextToLoadByViewing();
                            if (nextToLoad != null) {
                                synchronized (nextToLoad) {
                                    if (!nextToLoad.reloadHasBeenRequested()
                                            && !nextToLoad.hasRemovableSourceData()
                                            && (!(nextToLoad instanceof MapRegion) || !((MapRegion) nextToLoad).isRefreshing())) {
                                        shouldRequestLoading = true;
                                    }
                                }
                            } else {
                                shouldRequestLoading = true;
                            }

                            this.regionBuffer.clear();
                            int comparisonChunkX = this.playerChunkX - 16;
                            int comparisonChunkZ = this.playerChunkZ - 16;
                            LeveledRegion.setComparison(comparisonChunkX, comparisonChunkZ, 0, comparisonChunkX, comparisonChunkZ);

                            for (int visitRegionX = startRegionX; visitRegionX <= endRegionX; ++visitRegionX) {
                                for (int visitRegionZ = startRegionZ; visitRegionZ <= endRegionZ; ++visitRegionZ) {
                                    MapRegion visitRegion = this.mapProcessor.getMapRegion(this.writingLayer, visitRegionX, visitRegionZ, true);
                                    if (visitRegion != null && visitRegion.getLoadState() == 2) {
                                        visitRegion.registerVisit();
                                    }
                                    synchronized (visitRegion) {
                                        if (visitRegion.isResting()
                                                && shouldRequestLoading
                                                && !visitRegion.reloadHasBeenRequested()
                                                && !visitRegion.recacheHasBeenRequested()
                                                && (visitRegion.getLoadState() == 0 || visitRegion.getLoadState() == 4)) {
                                            visitRegion.calculateSortingChunkDistance();
                                            Misc.addToListOfSmallest(10, this.regionBuffer, visitRegion);
                                        }
                                    }
                                }
                            }

                            int toRequest = 1;
                            int counter = 0;

                            for (int i = 0; i < this.regionBuffer.size() && counter < toRequest; ++i) {
                                MapRegion region = this.regionBuffer.get(i);
                                if (region != nextToLoad || this.regionBuffer.size() <= 1) {
                                    synchronized (region) {
                                        if (!region.reloadHasBeenRequested()
                                                && !region.recacheHasBeenRequested()
                                                && (region.getLoadState() == 0 || region.getLoadState() == 4)) {
                                            region.setBeingWritten(true);
                                            this.mapProcessor.getMapSaveLoad().requestLoad(region, "writing");
                                            if (counter == 0) {
                                                this.mapProcessor.getMapSaveLoad().setNextToLoadByViewing((LeveledRegion<?>) region);
                                            }

                                            ++counter;
                                            if (region.getLoadState() == 4) {
                                                return;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    return;
                }
            }
        } catch (Throwable var39) {
            WorldMap.crashHandler.setCrashedBy(var39);
        }
    }


    @Inject(method = "writeChunk", at = @At(value = "HEAD"), cancellable = true)
    public void writeChunk(
            World world,
            Registry<Block> blockRegistry,
            int distance,
            boolean onlyLoad,
            Registry<Biome> biomeRegistry,
            OverlayManager overlayManager,
            boolean loadChunks,
            boolean updateChunks,
            boolean ignoreHeightmaps,
            boolean flowers,
            boolean detailedDebug,
            BlockPos.Mutable mutableBlockPos3,
            BlockTintProvider blockTintProvider,
            int caveDepth,
            int caveStart,
            int layerToWrite,
            int tileChunkX,
            int tileChunkZ,
            int tileChunkLocalX,
            int tileChunkLocalZ,
            int chunkX,
            int chunkZ,
            final CallbackInfoReturnable<Boolean> cir) {
        if (!XaeroPlusSettingRegistry.fastMapSetting.getValue()) return;

        final String cacheable = chunkX + " " + chunkZ;
        final Instant cacheValue = tileUpdateCache.getIfPresent(cacheable);
        if (nonNull(cacheValue)) {
            if (cacheValue.isBefore(Instant.now().minus(Duration.ofMillis((long) XaeroPlusSettingRegistry.mapWriterDelaySetting.getValue())))) {
                tileUpdateCache.put(cacheable, Instant.now());
            } else {
                cir.setReturnValue(false);
                cir.cancel();
            }
        } else {
            tileUpdateCache.put(cacheable, Instant.now());
        }
    }
}
