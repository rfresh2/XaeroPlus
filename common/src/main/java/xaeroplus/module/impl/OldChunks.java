package xaeroplus.module.impl;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import net.lenni0451.lambdaevents.EventHandler;
import net.minecraft.client.Minecraft;
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
import xaeroplus.event.ClientTickEvent;
import xaeroplus.event.XaeroWorldChangeEvent;
import xaeroplus.feature.render.ChunkHighlightProvider;
import xaeroplus.feature.render.ColorHelper;
import xaeroplus.feature.render.highlights.ChunkHighlightCache;
import xaeroplus.feature.render.highlights.ChunkHighlightLocalCache;
import xaeroplus.feature.render.highlights.ChunkHighlightSavingCache;
import xaeroplus.module.Module;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.util.ChunkScanner;
import xaeroplus.util.ChunkUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.Arrays.asList;
import static net.minecraft.world.level.Level.*;
import static xaeroplus.feature.render.ColorHelper.getColor;

public class OldChunks extends Module {
    private ChunkHighlightCache oldChunksCache = new ChunkHighlightLocalCache();
    private ChunkHighlightCache modernChunksCache = new ChunkHighlightLocalCache();
    private static final String OLD_CHUNKS_DATABASE_NAME = "XaeroPlusOldChunks";
    private static final String MODERN_CHUNKS_DATABASE_NAME = "XaeroPlusModernChunks";
    private int oldChunksColor = getColor(0, 0, 255, 100);
    private final Minecraft mc = Minecraft.getInstance();
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

    public void setOldChunksCache(boolean disk) {
        try {
            Long2LongMap map = oldChunksCache.getHighlightsState();
            oldChunksCache.onDisable();
            if (disk) {
                oldChunksCache = new ChunkHighlightSavingCache(OLD_CHUNKS_DATABASE_NAME);
            } else {
                oldChunksCache = new ChunkHighlightLocalCache();
            }
            if (this.isEnabled()) {
                oldChunksCache.onEnable();
                if (map != null) oldChunksCache.loadPreviousState(map);
            }
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error closing old chunks cache", e);
        }
        try {
            Long2LongMap map = modernChunksCache.getHighlightsState();
            modernChunksCache.onDisable();
            if (disk) {
                modernChunksCache = new ChunkHighlightSavingCache(MODERN_CHUNKS_DATABASE_NAME);
            } else {
                modernChunksCache = new ChunkHighlightLocalCache();
            }
            if (this.isEnabled()) {
                modernChunksCache.onEnable();
                if (map != null) modernChunksCache.loadPreviousState(map);
            }
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error closing modern chunks cache", e);
        }
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

    private boolean searchChunk(final ChunkAccess chunk) throws InterruptedException {
        ResourceKey<Level> actualDimension = ChunkUtils.getActualDimension();
        var x = chunk.getPos().x;
        var z = chunk.getPos().z;
        if (actualDimension == OVERWORLD || actualDimension == NETHER) {
            if (ChunkScanner.chunkContainsBlocks(chunk, actualDimension == OVERWORLD ? OVERWORLD_BLOCKS : NETHER_BLOCKS, 5)) {
                return modernChunksCache.addHighlight(x, z);
            } else {
                return oldChunksCache.addHighlight(x, z);
            }
        } else if (actualDimension == END) {
            Holder<Biome> biome = mc.level.getBiome(new BlockPos(ChunkUtils.chunkCoordToCoord(x) + 8, 64, ChunkUtils.chunkCoordToCoord(z) + 8));
            var biomeKey = biome.unwrapKey().get();
            if (biomeKey == Biomes.PLAINS) return false; // mitigate race condition where biomes aren't loaded yet for some reason
            if (biomeKey == Biomes.THE_END) {
                return oldChunksCache.addHighlight(x, z);
            } else {
                return modernChunksCache.addHighlight(x, z);
            }
        }
        return true;
    }

    @EventHandler
    public void onXaeroWorldChangeEvent(final XaeroWorldChangeEvent event) {
        oldChunksCache.handleWorldChange();
        modernChunksCache.handleWorldChange();
    }

    public boolean inUnknownDimension() {
        final ResourceKey<Level> dim = ChunkUtils.getActualDimension();
        return dim != OVERWORLD && dim != NETHER && dim != END;
    }

    @EventHandler
    public void onClientTickEvent(final ClientTickEvent.Post event) {
        oldChunksCache.handleTick();
        modernChunksCache.handleTick();
    }

    @Override
    public void onEnable() {
        Globals.drawManager.registerChunkHighlightProvider(
            this.getClass(),
            new ChunkHighlightProvider(
                this::isHighlighted,
                this::getOldChunksColor
            ));
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
        Globals.drawManager.unregister(this.getClass());
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
        if (inverse)
            return isOldChunkInverse(chunkPosX, chunkPosZ, dimensionId);
        else
            return isOldChunk(chunkPosX, chunkPosZ, dimensionId);
    }

    public boolean isOldChunk(final int chunkPosX, final int chunkPosZ, final ResourceKey<Level> dimensionId) {
        return oldChunksCache.isHighlighted(chunkPosX, chunkPosZ, dimensionId);
    }

    public boolean isOldChunkInverse(final int chunkPosX, final int chunkPosZ, final ResourceKey<Level> dimensionId) {
        return modernChunksCache.isHighlighted(chunkPosX, chunkPosZ, dimensionId);
    }

    public void setInverse(final Boolean b) {
        this.inverse = b;
    }
}
