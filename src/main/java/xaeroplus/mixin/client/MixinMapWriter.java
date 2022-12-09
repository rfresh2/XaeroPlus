package xaeroplus.mixin.client;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockGlass;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
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
import xaero.map.biome.WriterBiomeInfoSupplier;
import xaero.map.cache.BlockStateColorTypeCache;
import xaero.map.core.XaeroWorldMapCore;
import xaero.map.misc.Misc;
import xaero.map.region.MapBlock;
import xaero.map.region.MapRegion;
import xaero.map.region.OverlayBuilder;
import xaero.map.region.OverlayManager;
import xaeroplus.XaeroPlusSettingRegistry;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static java.util.Objects.nonNull;

@Mixin(value = MapWriter.class, remap = false)
public abstract class MixinMapWriter {
    @Shadow
    private OverlayBuilder overlayBuilder;
    @Shadow
    private int topH;
    @Shadow
    private int firstTransparentStateY;
    @Shadow
    private WriterBiomeInfoSupplier writerBiomeInfoSupplier;
    @Shadow
    private int[] biomeBuffer;
    @Shadow
    private BlockStateColorTypeCache colorTypeCache;
    @Shadow
    private MapProcessor mapProcessor;
    @Shadow
    @Final
    private BlockPos.MutableBlockPos mutableLocalPos;
    @Shadow
    @Final
    private BlockPos.MutableBlockPos mutableGlobalPos;
    @Shadow
    private int endTileChunkX;
    @Shadow
    private int endTileChunkZ;
    @Shadow
    private int startTileChunkX;
    @Shadow
    private int startTileChunkZ;
    @Shadow
    private long lastWriteTry;
    @Shadow
    private long lastWrite;
    @Shadow
    private int writeFreeSizeTiles;
    @Shadow
    private int writeFreeFullUpdateTargetTime;
    @Shadow
    private int workingFrameCount;
    @Shadow
    private long framesFreedTime = -1L;
    @Shadow
    public long writeFreeSinceLastWrite;
    @Final
    @Shadow
    private BlockPos.MutableBlockPos mutableBlockPos3;
    @Shadow
    public abstract boolean writeMap(
            World world,
            double playerX,
            double playerY,
            double playerZ,
            BiomeColorCalculator biomeColorCalculator,
            OverlayManager overlayManager,
            boolean loadChunks,
            boolean updateChunks,
            boolean ignoreHeightmaps,
            boolean flowers,
            boolean detailedDebug,
            BlockPos.MutableBlockPos mutableBlockPos3
    );

    // insert our own limiter on new tiles being written but this one's keyed on the actual chunk
    // tile "writes" also include a lot of extra operations and lookups before any writing is actually done
    // when we remove existing limiters those extra operations add up to a lot of unnecessary cpu time
    private final Cache<String, Instant> tileUpdateCache = CacheBuilder.newBuilder()
            // this delay seems to be fine enough even at high speeds, its equal to 1 tick in-game so still shouldn't miss any chunks
            // I would usually expect even second long expiration here to be fine
            // but there are some operations that make repeat invocations actually required
            // perhaps another time ill rewrite those. Or make the cache lock more aware of when we don't have any new updates to write/load
            // there's still alot of performance and efficiency on the table to regain
            // but i think this is a good middle ground for now
            .maximumSize(100)
            .build();

    protected MixinMapWriter() {
    }

    @Shadow
    protected abstract boolean shouldOverlayCached(IBlockState state);

    @Shadow
    public abstract boolean hasVanillaColor(IBlockState state, World world, BlockPos pos);

    @Shadow
    public abstract boolean isInvisible(World world, IBlockState state, Block b, boolean flowers);

    @Shadow
    public abstract boolean isGlowing(IBlockState state);

    /**
     * @author Entropy5
     * @reason obsidian roof
     */
    @Overwrite
    public boolean shouldOverlay(IBlockState state) {
        if (!(state.getBlock() instanceof BlockAir) && !(state.getBlock() instanceof BlockGlass) && state.getBlock().getBlockLayer() != BlockRenderLayer.TRANSLUCENT) {
            if (!(state.getBlock() instanceof BlockLiquid)) {
                return false;
            } else {
                int lightOpacity = state.getLightOpacity(this.mapProcessor.getWorld(), BlockPos.ORIGIN);
                return lightOpacity != 0;  // deleted argument to render water under obsidian roof regardless of light opacity
            }
        } else {
            return true;
        }
    }

    /**
     * @author Entropy5
     * @reason obsidian roof
     */
    @Overwrite
    public void loadPixel(World world, MapBlock pixel, MapBlock currentPixel, Chunk bchunk, int insideX, int insideZ, int highY, int lowY, boolean cave, boolean canReuseBiomeColours, boolean flowers, BlockPos.MutableBlockPos mutableBlockPos3) {
        pixel.prepareForWriting();
        this.overlayBuilder.startBuilding();
        boolean underair = !cave;
        IBlockState opaqueState = null;
        byte workingLight = -1;
        this.topH = lowY;
        this.mutableGlobalPos.setPos((bchunk.getPos().x << 4) + insideX, lowY - 1, (bchunk.getPos().z << 4) + insideZ);
        boolean shouldExtendTillTheBottom = false;
        int transparentSkipY = 0;
        boolean columnRoofObsidian = false;

        int h;
        IBlockState state;
        for (h = highY; h >= lowY; h = shouldExtendTillTheBottom ? transparentSkipY : h - 1) {
            this.mutableLocalPos.setPos(insideX, h, insideZ);
            state = bchunk.getBlockState(this.mutableLocalPos);
            Block b = state.getBlock();
            boolean roofObsidian = (h > 253 && b == Blocks.OBSIDIAN);
            if (roofObsidian & !columnRoofObsidian) {
                columnRoofObsidian = true;
            }
            shouldExtendTillTheBottom = !shouldExtendTillTheBottom && !this.overlayBuilder.isEmpty() && this.firstTransparentStateY - h >= 5 && !columnRoofObsidian;
            if (shouldExtendTillTheBottom) {
                for (transparentSkipY = h - 1; transparentSkipY >= lowY; --transparentSkipY) {
                    IBlockState traceState = bchunk.getBlockState(mutableBlockPos3.setPos(insideX, transparentSkipY, insideZ));
                    if (!this.shouldOverlayCached(traceState)) {
                        break;
                    }
                }
            }
            if (b instanceof BlockAir) {
                underair = true;
            } else if (underair) {
                this.mutableGlobalPos.setY(h);
                this.mutableLocalPos.setY(Math.min(255, h + 1));
                workingLight = (byte) bchunk.getLightFor(EnumSkyBlock.BLOCK, this.mutableLocalPos);
                if (!this.isInvisible(world, state, b, flowers)) {
                    if (this.shouldOverlayCached(state) || roofObsidian) {
                        if (h > this.topH) {
                            this.topH = h;
                        }

                        if (this.overlayBuilder.isEmpty()) {
                            this.firstTransparentStateY = h;
                        }

                        if (shouldExtendTillTheBottom) {
                            this.overlayBuilder.getCurrentOverlay().increaseOpacity(Misc.getStateById(this.overlayBuilder.getCurrentOverlay().getState()).getLightOpacity(world, this.mutableGlobalPos) * (h - transparentSkipY));
                        } else {
                            this.writerBiomeInfoSupplier.set(currentPixel, canReuseBiomeColours);
                            int stateId = Block.getStateId(state);
                            int opacity = roofObsidian ? 10 : b.getLightOpacity(state, world, this.mutableGlobalPos);
                            this.overlayBuilder.build(stateId, this.biomeBuffer, opacity, workingLight, world, this.mapProcessor, this.mutableGlobalPos, this.overlayBuilder.getOverlayBiome(), this.colorTypeCache, this.writerBiomeInfoSupplier);
                        }
                    } else if (this.hasVanillaColor(state, world, this.mutableGlobalPos)) {
                        if (h > this.topH) {
                            this.topH = h;
                        }

                        opaqueState = state;
                        break;
                    }
                }
            }
        }

        if (h < lowY) {
            h = lowY;
        }

        state = opaqueState == null ? Blocks.AIR.getDefaultState() : opaqueState;
        int stateId = Block.getStateId(state);
        byte light = opaqueState == null ? 0 : workingLight;
        this.overlayBuilder.finishBuilding(pixel);
        if (canReuseBiomeColours && currentPixel != null && currentPixel.getState() == stateId) {
            this.biomeBuffer[0] = currentPixel.getColourType();
            this.biomeBuffer[1] = currentPixel.getBiome();
            this.biomeBuffer[2] = currentPixel.getCustomColour();
        } else {
            this.colorTypeCache.getBlockBiomeColour(world, state, this.mutableGlobalPos, this.biomeBuffer, -1);
        }

        if (this.overlayBuilder.getOverlayBiome() != -1) {
            this.biomeBuffer[1] = this.overlayBuilder.getOverlayBiome();
        }

        boolean glowing = this.isGlowing(state);
        pixel.write(stateId, h, this.topH, this.biomeBuffer, light, glowing, cave);
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
                synchronized(this.mapProcessor.renderThreadPauseSync) {
                    if (!this.mapProcessor.isWritingPaused()
                            && !this.mapProcessor.isWaitingForWorldUpdate()
                            && this.mapProcessor.getMapSaveLoad().isRegionDetectionComplete()
                            && this.mapProcessor.isCurrentMultiworldWritable()) {
                        if (this.mapProcessor.getWorld() == null || !this.mapProcessor.caveStartIsDetermined() || this.mapProcessor.isCurrentMapLocked()) {
                            return;
                        }

                        if (this.mapProcessor.getCurrentWorldId() != null
                                && !this.mapProcessor.ignoreWorld(this.mapProcessor.getWorld())
                                && (WorldMap.settings.updateChunks || WorldMap.settings.loadChunks)) {
                            double playerX;
                            double playerY;
                            double playerZ;
                            synchronized(this.mapProcessor.mainStuffSync) {
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

                            long sinceLastWrite = Math.min(passed, Math.max(this.writeFreeSinceLastWrite, 1));
                            if (this.framesFreedTime != -1L) {
                                sinceLastWrite = time - this.framesFreedTime;
                            }

                            long tilesToUpdate = XaeroPlusSettingRegistry.fastMapSetting.getBooleanSettingValue()
                                    ? sizeTiles /** always write all tiles **/
                                    : Math.min(sinceLastWrite * (long)sizeTiles / (long)fullUpdateTargetTime, 100L); // default

                            if (this.lastWrite == -1L || tilesToUpdate != 0L) {
                                this.lastWrite = time;
                            }

                            if (tilesToUpdate != 0L) {
                                if (this.framesFreedTime != -1L) {
                                    this.writeFreeSinceLastWrite = sinceLastWrite;
                                    this.framesFreedTime = -1L;
                                } else {
                                    int timeLimit = (int)(Math.min(sinceLastWrite, 50L) * 86960L);
                                    long writeStartNano = System.nanoTime();
                                    boolean loadChunks = WorldMap.settings.loadChunks;
                                    boolean updateChunks = WorldMap.settings.updateChunks;
                                    boolean ignoreHeightmaps = this.mapProcessor.getMapWorld().isIgnoreHeightmaps();
                                    boolean flowers = WorldMap.settings.flowers;
                                    boolean detailedDebug = WorldMap.settings.detailed_debug;
                                    BlockPos.MutableBlockPos mutableBlockPos3 = this.mutableBlockPos3;

                                    for(int i = 0; (long)i < tilesToUpdate; ++i) {
                                        if (this.writeMap(
                                                this.mapProcessor.getWorld(),
                                                playerX,
                                                playerY,
                                                playerZ,
                                                biomeColorCalculator,
                                                overlayManager,
                                                loadChunks,
                                                updateChunks,
                                                ignoreHeightmaps,
                                                flowers,
                                                detailedDebug,
                                                mutableBlockPos3
                                        )) {
                                            --i;
                                        }

                                        /** removing time limit **/
                                        if (!XaeroPlusSettingRegistry.fastMapSetting.getBooleanSettingValue()) {
                                            if (System.nanoTime() - writeStartNano >= (long)timeLimit) {
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

                            for(int visitRegionX = startRegionX; visitRegionX <= endRegionX; ++visitRegionX) {
                                for(int visitRegionZ = startRegionZ; visitRegionZ <= endRegionZ; ++visitRegionZ) {
                                    MapRegion visitRegion = this.mapProcessor.getMapRegion(visitRegionX, visitRegionZ, false);
                                    if (visitRegion != null && visitRegion.getLoadState() == 2) {
                                        visitRegion.registerVisit();
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
    public void writeChunk(World world, int distance, boolean onlyLoad, BiomeColorCalculator biomeColorCalculator, OverlayManager overlayManager, boolean loadChunks, boolean updateChunks, boolean ignoreHeightmaps, boolean flowers, boolean detailedDebug, BlockPos.MutableBlockPos mutableBlockPos3, int tileChunkX, int tileChunkZ, int tileChunkLocalX, int tileChunkLocalZ, int chunkX, int chunkZ, CallbackInfoReturnable<Boolean> cir) {
        if (!XaeroPlusSettingRegistry.fastMapSetting.getBooleanSettingValue()) return;

        final String cacheable = chunkX + " " + chunkZ;
        final Instant cacheValue = tileUpdateCache.getIfPresent(cacheable);
        if (nonNull(cacheValue)) {
            if (cacheValue.isBefore(Instant.now().minus((long) XaeroPlusSettingRegistry.mapWriterDelaySetting.getFloatSettingValue(), ChronoUnit.MILLIS))) {
                tileUpdateCache.invalidate(cacheable);
            } else {
                cir.setReturnValue(false);
                cir.cancel();
            }
        } else {
            tileUpdateCache.put(cacheable, Instant.now());
        }
    }
}
