package xaeroplus.module.impl;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
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
import xaeroplus.event.ChunkDataEvent;
import xaeroplus.feature.render.highlights.SavableHighlightCacheInstance;
import xaeroplus.module.Module;
import xaeroplus.settings.Settings;
import xaeroplus.util.ChunkScanner;
import xaeroplus.util.ChunkUtils;
import xaeroplus.util.ColorHelper;

import static net.minecraft.world.level.Level.*;
import static xaeroplus.util.ColorHelper.getColor;

public class OldChunks extends Module {
    public final SavableHighlightCacheInstance oldChunksCache = new SavableHighlightCacheInstance("XaeroPlusOldChunks");
    public final SavableHighlightCacheInstance modernChunksCache = new SavableHighlightCacheInstance("XaeroPlusModernChunks");
    private int oldChunksColor = getColor(0, 0, 255, 100);

    private boolean inverse = false;
    private static final ReferenceSet<Block> OVERWORLD_BLOCKS = ReferenceOpenHashSet.of(
        Blocks.COPPER_ORE,
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
        Blocks.CAVE_VINES_PLANT);
    private static final ReferenceSet<Block> NETHER_BLOCKS = ReferenceOpenHashSet.of(
        Blocks.ANCIENT_DEBRIS,
        Blocks.BLACKSTONE,
        Blocks.BASALT,
        Blocks.CRIMSON_NYLIUM,
        Blocks.WARPED_NYLIUM,
        Blocks.NETHER_GOLD_ORE,
        Blocks.CHAIN
    );

    public void setDiskCache(boolean disk) {
        oldChunksCache.setDiskCache(disk, isEnabled());
        modernChunksCache.setDiskCache(disk, isEnabled());
    }

    @EventHandler
    public void onChunkData(final ChunkDataEvent event) {
        if (event.seenChunk()) return;
        searchChunk(event.chunk());
    }

    private void searchChunk(final ChunkAccess chunk) {
        ResourceKey<Level> actualDimension = ChunkUtils.getActualDimension();
        var x = chunk.getPos().x;
        var z = chunk.getPos().z;
        if (actualDimension == OVERWORLD || actualDimension == NETHER) {
            if (ChunkScanner.chunkContainsBlocks(chunk, actualDimension == OVERWORLD ? OVERWORLD_BLOCKS : NETHER_BLOCKS, 5))
                modernChunksCache.get().addHighlight(x, z);
            else
                oldChunksCache.get().addHighlight(x, z);
        } else if (actualDimension == END) {
            Holder<Biome> biomeHolder = mc.level.getBiome(new BlockPos(ChunkUtils.chunkCoordToCoord(x) + 8, 64, ChunkUtils.chunkCoordToCoord(z) + 8));
            if (biomeHolder.unwrapKey().filter(biome -> biome.equals(Biomes.THE_END)).isEmpty())
                modernChunksCache.get().addHighlight(x, z);
            else
                oldChunksCache.get().addHighlight(x, z);
        }
    }

    public boolean inUnknownDimension() {
        final ResourceKey<Level> dim = ChunkUtils.getActualDimension();
        return dim != OVERWORLD && dim != NETHER && dim != END;
    }

    @Override
    public void onEnable() {
        Globals.drawManager.registry().registerDirectChunkHighlightProvider(
            this.getClass().getName(),
            this::getHighlightsState,
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
                if (chunk instanceof EmptyLevelChunk || chunk == null) continue;
                searchChunk(chunk);
            }
        }
    }

    @Override
    public void onDisable() {
        oldChunksCache.onDisable();
        modernChunksCache.onDisable();
        Globals.drawManager.registry().unregisterChunkHighlightProvider(this.getClass().getName());
    }

    public int getOldChunksColor() {
        return oldChunksColor;
    }

    public void setRgbColor(final int color) {
        oldChunksColor = ColorHelper.getColorWithAlpha(color, Settings.REGISTRY.oldChunksAlphaSetting.getAsInt());
    }

    public void setAlpha(final double a) {
        oldChunksColor = ColorHelper.getColorWithAlpha(oldChunksColor, (int) (a));
    }

    public boolean isHighlighted(final int chunkPosX, final int chunkPosZ, final ResourceKey<Level> dimensionId) {
        return inverse
            ? isOldChunkInverse(chunkPosX, chunkPosZ, dimensionId)
            : isOldChunk(chunkPosX, chunkPosZ, dimensionId);
    }

    public Long2LongMap getHighlightsState(final ResourceKey<Level> dimension) {
        return inverse
            ? modernChunksCache.get().getCacheMap(dimension)
            : oldChunksCache.get().getCacheMap(dimension);
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
