package xaeroplus.mixin.client;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.state.State;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
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
import xaeroplus.util.ChunkUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.nonNull;
import static net.minecraft.world.World.NETHER;

@Mixin(value = MapWriter.class, remap = false)
public abstract class MixinMapWriter {
    // insert our own limiter on new tiles being written but this one's keyed on the actual chunk
    // tile "writes" also include a lot of extra operations and lookups before any writing is actually done
    // when we remove existing limiters those extra operations add up to a lot of unnecessary cpu time
    private final Cache<Long, Long> tileUpdateCache = Caffeine.newBuilder()
            // I would usually expect even second long expiration here to be fine
            // but there are some operations that make repeat invocations actually required
            // perhaps another time ill rewrite those. Or make the cache lock more aware of when we don't have any new updates to write/load
            // there's still alot of performance and efficiency on the table to regain
            // but i think this is a good middle ground for now
            .maximumSize(10000)
            .expireAfterWrite(5L, TimeUnit.SECONDS)
            .<Long, Long>build();
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
    protected abstract boolean loadPixelHelp(
            MapBlock pixel,
            MapBlock currentPixel,
            World world,
            Registry<Block> blockRegistry,
            BlockState state,
            byte light,
            byte skyLight,
            WorldChunk bchunk,
            int insideX,
            int insideZ,
            int h,
            boolean canReuseBiomeColours,
            boolean cave,
            FluidState fluidFluidState,
            Registry<Biome> biomeRegistry,
            int transparentSkipY,
            boolean shouldExtendTillTheBottom,
            boolean flowers,
            boolean underair
    );

    @Shadow
    public abstract boolean hasVanillaColor(BlockState state, World world, Registry<Block> blockRegistry, BlockPos pos);

    @Shadow
    public abstract boolean isInvisible(World world, BlockState state, Block b, boolean flowers);

    @Shadow
    public abstract boolean isGlowing(BlockState state);

    @Shadow
    protected abstract BlockState unpackFramedBlocks(BlockState original, World world, BlockPos globalPos);

    /**
     * @author Entropy5
     * @reason obsidian roof
     */
    @Inject(method = "loadPixel", at = @At("HEAD"), cancellable = true, remap = true)
    public void loadPixel(final World world,
                          final Registry<Block> blockRegistry,
                          final MapBlock pixel,
                          final MapBlock currentPixel,
                          final WorldChunk bchunk,
                          final int insideX,
                          final int insideZ,
                          final int highY,
                          final int lowY,
                          final boolean cave,
                          final boolean fullCave,
                          final int mappedHeight,
                          final boolean canReuseBiomeColours,
                          final boolean ignoreHeightmaps,
                          final Registry<Biome> biomeRegistry,
                          final boolean flowers,
                          final int worldBottomY,
                          final BlockPos.Mutable mutableBlockPos3,
                          final CallbackInfo ci) {
        if (!XaeroPlusSettingRegistry.transparentObsidianRoofSetting.getValue()) {
            return;
        } else {
            ci.cancel();
        }
        pixel.prepareForWriting(worldBottomY);
        this.overlayBuilder.startBuilding();
        boolean underair = !cave || fullCave;
        boolean shouldEnterGround = fullCave;
        BlockState opaqueState = null;
        byte workingLight = -1;
        boolean worldHasSkyLight = world.getDimension().hasSkyLight();
        byte workingSkyLight = (byte)(worldHasSkyLight ? 15 : 0);
        this.topH = lowY;
        this.mutableGlobalPos.set((bchunk.getPos().x << 4) + insideX, lowY - 1, (bchunk.getPos().z << 4) + insideZ);
        boolean shouldExtendTillTheBottom = false;
        int transparentSkipY = 0;
        boolean columnRoofObsidian = false;

        int h;
        for(h = highY; h >= lowY; h = shouldExtendTillTheBottom ? transparentSkipY : h - 1) {
            this.mutableLocalPos.set(insideX, h, insideZ);
            BlockState state = bchunk.getBlockState(this.mutableLocalPos);
            if (state == null) {
                state = Blocks.AIR.getDefaultState();
            }

            this.mutableGlobalPos.setY(h);
            state = this.unpackFramedBlocks(state, world, this.mutableGlobalPos);
            FluidState fluidFluidState = state.getFluidState();
            Block b = state.getBlock();
            boolean roofObsidian = (h >= XaeroPlusSettingRegistry.transparentObsidianRoofYSetting.getValue() && b == Blocks.OBSIDIAN);
            if (roofObsidian && XaeroPlusSettingRegistry.transparentObsidianRoofDarkeningSetting.getValue() == 0) {
                transparentSkipY--;
                continue;  // skip over obsidian roof completely
            }
            if (roofObsidian & !columnRoofObsidian) {
                columnRoofObsidian = true;
            }
            shouldExtendTillTheBottom = !shouldExtendTillTheBottom && !this.overlayBuilder.isEmpty() && this.firstTransparentStateY - h >= 5 && !columnRoofObsidian;
            if (shouldExtendTillTheBottom) {
                for(transparentSkipY = h - 1; transparentSkipY >= lowY; --transparentSkipY) {
                    BlockState traceState = bchunk.getBlockState(mutableBlockPos3.set(insideX, transparentSkipY, insideZ));
                    if (traceState == null) {
                        traceState = Blocks.AIR.getDefaultState();
                    }
                    FluidState traceFluidState = traceState.getFluidState();
                    if (!traceFluidState.isEmpty()) {
                        if (!this.shouldOverlayCached(traceFluidState)) {
                            break;
                        }

                        if (!(traceState.getBlock() instanceof AirBlock)
                                && traceState.getBlock() == ((BlockState)this.fluidToBlock.apply(traceFluidState)).getBlock()) {
                            continue;
                        }
                    }

                    if (!this.shouldOverlayCached(traceState)) {
                        break;
                    }
                }
            }

            this.mutableGlobalPos.setY(h + 1);
            workingLight = (byte)world.getLightLevel(LightType.BLOCK, this.mutableGlobalPos);
            if (cave && workingLight < 15 && worldHasSkyLight) {
                if (!ignoreHeightmaps && !fullCave && highY >= mappedHeight) {
                    workingSkyLight = 15;
                } else {
                    workingSkyLight = (byte)world.getLightLevel(LightType.SKY, this.mutableGlobalPos);
                }
            }

            this.mutableGlobalPos.setY(h);
            if (!fluidFluidState.isEmpty() && (!cave || !shouldEnterGround)) {
                underair = true;
                BlockState fluidState = (BlockState)this.fluidToBlock.apply(fluidFluidState);
                if (this.loadPixelHelp(
                        pixel,
                        currentPixel,
                        world,
                        blockRegistry,
                        fluidState,
                        workingLight,
                        workingSkyLight,
                        bchunk,
                        insideX,
                        insideZ,
                        h,
                        canReuseBiomeColours,
                        cave,
                        fluidFluidState,
                        biomeRegistry,
                        transparentSkipY,
                        shouldExtendTillTheBottom,
                        flowers,
                        underair
                )) {
                    opaqueState = state;
                    break;
                }
            }

            if (b instanceof AirBlock) {
                underair = true;
            } else if (underair && state.getBlock() != ((BlockState)this.fluidToBlock.apply(fluidFluidState)).getBlock()) {
                if (cave && shouldEnterGround) {
                    if (!state.isBurnable() && !state.isReplaceable() && state.getPistonBehavior() != PistonBehavior.DESTROY && !this.shouldOverlayCached(state)) {
                        underair = false;
                        shouldEnterGround = false;
                    }
                } else if (this.loadPixelHelp(
                        pixel,
                        currentPixel,
                        world,
                        blockRegistry,
                        state,
                        workingLight,
                        workingSkyLight,
                        bchunk,
                        insideX,
                        insideZ,
                        h,
                        canReuseBiomeColours,
                        cave,
                        null,
                        biomeRegistry,
                        transparentSkipY,
                        shouldExtendTillTheBottom,
                        flowers,
                        underair
                )) {
                    opaqueState = state;
                    break;
                }
            }
        }

        if (h < lowY) {
            h = lowY;
        }

        RegistryKey<Biome> blockBiome = null;
        BlockState state = opaqueState == null ? Blocks.AIR.getDefaultState() : opaqueState;
        this.overlayBuilder.finishBuilding(pixel);
        byte light = 0;
        if (opaqueState != null) {
            light = workingLight;
            if (cave && workingLight < 15 && pixel.getNumberOfOverlays() == 0 && workingSkyLight > workingLight) {
                light = workingSkyLight;
            }
        } else {
            h = worldBottomY;
        }

        if (canReuseBiomeColours && currentPixel != null && currentPixel.getState() == state && currentPixel.getTopHeight() == this.topH) {
            blockBiome = currentPixel.getBiome();
        } else {
            this.mutableGlobalPos.setY(this.topH);
            blockBiome = this.biomeGetter.getBiome(world, this.mutableGlobalPos, biomeRegistry);
            this.mutableGlobalPos.setY(h);
        }

        if (this.overlayBuilder.getOverlayBiome() != null) {
            blockBiome = this.overlayBuilder.getOverlayBiome();
        }

        boolean glowing = this.isGlowing(state);
        pixel.write(state, h, this.topH, blockBiome, light, glowing, cave);
    }

    @Inject(method = "loadPixelHelp", at = @At(value = "HEAD"), cancellable = true, remap = true)
    public void loadPixelHelpInject(final MapBlock pixel,
                                    final MapBlock currentPixel,
                                    final World world,
                                    final Registry<Block> blockRegistry,
                                    final BlockState state,
                                    final byte light,
                                    final byte skyLight,
                                    final WorldChunk bchunk,
                                    final int insideX,
                                    final int insideZ,
                                    final int h,
                                    final boolean canReuseBiomeColours,
                                    final boolean cave,
                                    final FluidState fluidFluidState,
                                    final Registry<Biome> biomeRegistry,
                                    final int transparentSkipY,
                                    final boolean shouldExtendTillTheBottom,
                                    final boolean flowers,
                                    final boolean underair,
                                    final CallbackInfoReturnable<Boolean> cir) {
        if (!XaeroPlusSettingRegistry.transparentObsidianRoofSetting.getValue()) {
            return;
        } else {
            cir.cancel();
        }

        Block b = state.getBlock();
        boolean roofObsidian = b == Blocks.OBSIDIAN && h > XaeroPlusSettingRegistry.transparentObsidianRoofYSetting.getValue();
        if (this.isInvisible(world, state, b, flowers)) {
            cir.setReturnValue(false);
        } else if (this.shouldOverlayCached(fluidFluidState == null ? state : fluidFluidState) || roofObsidian) {
            if (cave && !underair) {
                cir.setReturnValue(false);
            } else {
                if (h > this.topH) {
                    this.topH = h;
                }

                byte overlayLight = light;
                if (this.overlayBuilder.isEmpty()) {
                    this.firstTransparentStateY = h;
                    if (cave && skyLight > light) {
                        overlayLight = skyLight;
                    }
                }

                if (shouldExtendTillTheBottom) {
                    this.overlayBuilder
                            .getCurrentOverlay()
                            .increaseOpacity(this.overlayBuilder.getCurrentOverlay().getState().getOpacity(world, this.mutableGlobalPos) * (h - transparentSkipY));
                } else {
                    RegistryKey<Biome> overlayBiome = this.overlayBuilder.getOverlayBiome();
                    if (overlayBiome == null) {
                        if (canReuseBiomeColours
                                && currentPixel != null
                                && currentPixel.getNumberOfOverlays() > 0
                                && currentPixel.getOverlays().get(0).getState() == state) {
                            overlayBiome = currentPixel.getBiome();
                        } else {
                            overlayBiome = this.biomeGetter.getBiome(world, this.mutableGlobalPos, biomeRegistry);
                        }
                    }
                    int opacity = roofObsidian ? 5 : state.getOpacity(world, this.mutableGlobalPos);
                    this.overlayBuilder.build(state, opacity, overlayLight, this.mapProcessor, overlayBiome);
                }

                cir.setReturnValue(false);
            }
        } else if (!this.hasVanillaColor(state, world, blockRegistry, this.mutableGlobalPos)) {
            cir.setReturnValue(false);
        } else if (cave && !underair) {
            cir.setReturnValue(true);
        } else {
            if (h > this.topH) {
                this.topH = h;
            }

            cir.setReturnValue(true);
        }
    }

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

                            long tilesToUpdate = (XaeroPlusSettingRegistry.fastMapSetting.getValue() && this.mapProcessor.getCurrentCaveLayer() == Integer.MAX_VALUE)
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
                                        if (!XaeroPlusSettingRegistry.fastMapSetting.getValue() && this.mapProcessor.getCurrentCaveLayer() == Integer.MAX_VALUE) {
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


    @Inject(method = "writeChunk", at = @At(value = "HEAD"), cancellable = true, remap = true)
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

        final Long cacheable = ChunkUtils.chunkPosToLong(chunkX, chunkZ);
        final Long cacheValue = tileUpdateCache.getIfPresent(cacheable);
        if (nonNull(cacheValue)) {
            if (cacheValue < System.currentTimeMillis() - (long) XaeroPlusSettingRegistry.fastMapWriterDelaySetting.getValue()) {
                tileUpdateCache.put(cacheable, System.currentTimeMillis());
            } else {
                cir.setReturnValue(false);
                cir.cancel();
            }
        } else {
            tileUpdateCache.put(cacheable, System.currentTimeMillis());
        }
    }

    @Redirect(method = "writeChunk", at = @At(value = "INVOKE", target = "Lxaero/map/MapWriter;loadPixel(Lnet/minecraft/world/World;Lnet/minecraft/registry/Registry;Lxaero/map/region/MapBlock;Lxaero/map/region/MapBlock;Lnet/minecraft/world/chunk/WorldChunk;IIIIZZIZZLnet/minecraft/registry/Registry;ZILnet/minecraft/util/math/BlockPos$Mutable;)V"), remap = true)
    public void redirectLoadPixelForNetherFix(MapWriter instance,
                                              World world,
                                              Registry<Block> blockRegistry,
                                              MapBlock pixel,
                                              MapBlock currentPixel,
                                              WorldChunk bchunk,
                                              int insideX,
                                              int insideZ,
                                              int highY,
                                              int lowY,
                                              boolean cave,
                                              boolean fullCave,
                                              int mappedHeight,
                                              boolean canReuseBiomeColours,
                                              boolean ignoreHeightmaps,
                                              Registry<Biome> biomeRegistry,
                                              boolean flowers,
                                              int worldBottomY,
                                              BlockPos.Mutable mutableBlockPos3) {
        if (XaeroPlusSettingRegistry.netherCaveFix.getValue()) {
            final boolean nether = world.getRegistryKey() == NETHER;
            final boolean shouldForceFullInNether = !cave && nether;
            instance.loadPixel(world,
                               blockRegistry,
                               pixel,
                               currentPixel,
                               bchunk,
                               insideX,
                               insideZ,
                               highY,
                               lowY,
                               shouldForceFullInNether || cave,
                               shouldForceFullInNether || fullCave,
                               mappedHeight,
                               canReuseBiomeColours,
                               ignoreHeightmaps,
                               biomeRegistry,
                               flowers,
                               worldBottomY,
                               mutableBlockPos3);
        } else {
            instance.loadPixel(world,
                               blockRegistry,
                               pixel,
                               currentPixel,
                               bchunk,
                               insideX,
                               insideZ,
                               highY,
                               lowY,
                               cave,
                               fullCave,
                               mappedHeight,
                               canReuseBiomeColours,
                               ignoreHeightmaps,
                               biomeRegistry,
                               flowers,
                               worldBottomY,
                               mutableBlockPos3);
        }
    }
}
