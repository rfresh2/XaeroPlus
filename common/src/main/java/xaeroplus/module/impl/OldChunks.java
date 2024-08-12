package xaeroplus.module.impl;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import net.lenni0451.lambdaevents.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import xaeroplus.Globals;
import xaeroplus.XaeroPlus;
import xaeroplus.event.ChunkDataEvent;
import xaeroplus.feature.render.highlights.SavableHighlightCacheInstance;
import xaeroplus.module.Module;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.util.ChunkScanner;
import xaeroplus.util.ChunkUtils;
import xaeroplus.util.ColorHelper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.Arrays.asList;
import static net.minecraft.world.level.Level.*;
import static xaeroplus.util.ColorHelper.getColor;

public class OldChunks extends Module {
    private final SavableHighlightCacheInstance oldChunksCache = new SavableHighlightCacheInstance("XaeroPlusOldChunks");
    private final SavableHighlightCacheInstance modernChunksCache = new SavableHighlightCacheInstance("XaeroPlusModernChunks");
    private int oldChunksColor = getColor(0, 0, 255, 100);
    private final ExecutorService searchExecutor = Executors.newFixedThreadPool(
        1,
        new ThreadFactoryBuilder()
            .setNameFormat("XaeroPlus-OldChunks-Search-%d")
            .build());

    private boolean inverse = false;
    private static final ReferenceSet<Block> OVERWORLD_BLOCKS = new ReferenceOpenHashSet<>();
    private static final ReferenceSet<Block> NETHER_BLOCKS = new ReferenceOpenHashSet<>();
    static {
        OVERWORLD_BLOCKS.addAll(asList(Blocks.COPPER_ORE,
                                       Blocks.DEEPSLATE_COPPER_ORE,
                                       Blocks.AMETHYST_BLOCK,
                                       Blocks.SMOOTH_BASALT,
                                       Blocks.TUFF,
                                       Blocks.KELP,
                                       Blocks.KELP_PLANT,
                                       Blocks.POINTED_DRIPSTONE,
                                       Blocks.DRIPSTONE_BLOCK,
                                       Blocks.DEEPSLATE,
                                       Blocks.AZALEA,
                                       Blocks.BIG_DRIPLEAF,
                                       Blocks.BIG_DRIPLEAF_STEM,
                                       Blocks.SMALL_DRIPLEAF,
                                       Blocks.MOSS_BLOCK,
                                       Blocks.CAVE_VINES,
                                       Blocks.CAVE_VINES_PLANT));
        NETHER_BLOCKS.addAll(asList(Blocks.ANCIENT_DEBRIS,
                                    Blocks.BLACKSTONE,
                                    Blocks.BASALT,
                                    Blocks.CRIMSON_NYLIUM,
                                    Blocks.WARPED_NYLIUM,
                                    Blocks.NETHER_GOLD_ORE,
                                    Blocks.CHAIN));
    }

    public void setDiskCache(boolean disk) {
        oldChunksCache.setDiskCache(disk, isEnabled());
        modernChunksCache.setDiskCache(disk, isEnabled());
    }

    @EventHandler
    public void onChunkData(final ChunkDataEvent event) {
        if (event.seenChunk()) return;
        searchChunkAsync(event.chunk());
    }

    private void searchChunkAsync(final ChunkAccess chunk) {
        searchExecutor.submit(() -> {
            try {
                int iterations = 0;
                while (iterations++ < 3) {
                    if (searchChunk(chunk)) break;
                    Thread.sleep(500);
                }
                if (iterations == 3) {
                    XaeroPlus.LOGGER.info("[{}, {}] Too many search iterations", chunk.getPos().x, chunk.getPos().z);
                }
            } catch (final Throwable e) {
                XaeroPlus.LOGGER.error("Error searching for OldChunk in chunk: {}, {}", chunk.getPos().x, chunk.getPos().z, e);
            }
        });
    }

    private boolean searchChunk(final ChunkAccess chunk) {
        ResourceKey<Level> actualDimension = ChunkUtils.getActualDimension();
        var x = chunk.getPos().x;
        var z = chunk.getPos().z;
        if (actualDimension == OVERWORLD || actualDimension == NETHER) {
            return ChunkScanner.chunkContainsBlocks(chunk, actualDimension == OVERWORLD ? OVERWORLD_BLOCKS : NETHER_BLOCKS, 5)
                ? modernChunksCache.get().addHighlight(x, z)
                : oldChunksCache.get().addHighlight(x, z);
        } else if (actualDimension == END) {
            Holder<Biome> biome = mc.level.getBiome(new BlockPos(ChunkUtils.chunkCoordToCoord(x) + 8, 64, ChunkUtils.chunkCoordToCoord(z) + 8));
            var biomeKey = biome.unwrapKey().get();
            return biomeKey == Biomes.THE_END
                ? oldChunksCache.get().addHighlight(x, z)
                : modernChunksCache.get().addHighlight(x, z);
        }
        return true;
    }

    public boolean inUnknownDimension() {
        final ResourceKey<Level> dim = ChunkUtils.getActualDimension();
        return dim != OVERWORLD && dim != NETHER && dim != END;
    }

    @Override
    public void onEnable() {
        Globals.drawManager.registerChunkHighlightProvider(
            this.getClass(),
            this::getHighlightsSnapshot,
            this::getOldChunksColor);
        oldChunksCache.onEnable();
        modernChunksCache.onEnable();
        searchAllLoadedChunks();
    }

    private void searchAllLoadedChunks() {
        if (mc.level == null || inUnknownDimension()) return;
        final int renderDist = mc.options.renderDistance().get();
        final int xMin = ChunkUtils.getPlayerChunkX() - renderDist;
        final int xMax = ChunkUtils.getPlayerChunkX() + renderDist;
        final int zMin = ChunkUtils.getPlayerChunkZ() - renderDist;
        final int zMax = ChunkUtils.getPlayerChunkZ() + renderDist;
        for (int x = xMin; x <= xMax; x++) {
            for (int z = zMin; z <= zMax; z++) {
                ChunkAccess chunk = mc.level.getChunkSource().getChunk(x, z, false);
                if (chunk instanceof EmptyLevelChunk) continue;
                searchChunkAsync(chunk);
            }
        }
    }

    @Override
    public void onDisable() {
        oldChunksCache.onDisable();
        modernChunksCache.onDisable();
        Globals.drawManager.unregisterChunkHighlightProvider(this.getClass());
    }

    public int getOldChunksColor() {
        return oldChunksColor;
    }

    public void setRgbColor(final int color) {
        oldChunksColor = ColorHelper.getColorWithAlpha(color, (int) XaeroPlusSettingRegistry.oldChunksAlphaSetting.getValue());
    }

    public void setAlpha(final float a) {
        oldChunksColor = ColorHelper.getColorWithAlpha(oldChunksColor, (int) (a));
    }

    public boolean isHighlighted(final int chunkPosX, final int chunkPosZ, final ResourceKey<Level> dimensionId) {
        return inverse
            ? isOldChunkInverse(chunkPosX, chunkPosZ, dimensionId)
            : isOldChunk(chunkPosX, chunkPosZ, dimensionId);
    }

    public LongList getHighlightsSnapshot(final int windowRegionX, final int windowRegionZ, final int windowRegionSize, final ResourceKey<Level> dimension) {
        return inverse
            ? modernChunksCache.get().getHighlightsSnapshot(dimension)
            : oldChunksCache.get().getHighlightsSnapshot(dimension);
    }

    public boolean isOldChunk(final int chunkPosX, final int chunkPosZ, final ResourceKey<Level> dimensionId) {
        return oldChunksCache.get().isHighlighted(chunkPosX, chunkPosZ, dimensionId);
    }

    public boolean isOldChunkInverse(final int chunkPosX, final int chunkPosZ, final ResourceKey<Level> dimensionId) {
        return modernChunksCache.get().isHighlighted(chunkPosX, chunkPosZ, dimensionId);
    }

    public void setInverse(final Boolean b) {
        this.inverse = b;
    }
}
