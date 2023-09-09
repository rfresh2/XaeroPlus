package xaeroplus.module.impl;

import com.collarmc.pounce.Subscribe;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;
import xaeroplus.XaeroPlus;
import xaeroplus.event.ChunkDataEvent;
import xaeroplus.event.ClientTickEvent;
import xaeroplus.event.XaeroWorldChangeEvent;
import xaeroplus.module.Module;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.util.ChunkUtils;
import xaeroplus.util.ColorHelper;
import xaeroplus.util.MutableBlockPos;
import xaeroplus.util.highlights.ChunkHighlightCache;
import xaeroplus.util.highlights.ChunkHighlightLocalCache;
import xaeroplus.util.highlights.ChunkHighlightSavingCache;
import xaeroplus.util.highlights.HighlightAtChunkPos;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static net.minecraft.world.World.*;
import static xaeroplus.util.ColorHelper.getColor;

@Module.ModuleInfo()
public class OldChunks extends Module {
    private ChunkHighlightCache oldChunksCache = new ChunkHighlightLocalCache();
    private ChunkHighlightCache modernChunksCache = new ChunkHighlightLocalCache();
    private static final String OLD_CHUNKS_DATABASE_NAME = "XaeroPlusOldChunks";
    private static final String MODERN_CHUNKS_DATABASE_NAME = "XaeroPlusModernChunks";
    private int oldChunksColor = getColor(0, 0, 255, 100);
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final ExecutorService searchExecutor = Executors.newSingleThreadExecutor();
    private boolean inverse = false;

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
                oldChunksCache.loadPreviousState(map);
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
                modernChunksCache.loadPreviousState(map);
            }
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error closing modern chunks cache", e);
        }
    }

    @Subscribe
    public void onChunkData(final ChunkDataEvent event) {
        searchChunkAsync(event.chunk());
    }

    private void searchChunkAsync(final Chunk chunk) {
        searchExecutor.submit(() -> {
            try {
                int iterations = 0;
                while (iterations++ < 3) {
                    if (searchChunk(chunk)) break;
                    Thread.sleep(500);
                }
            } catch (final Throwable e) {
                XaeroPlus.LOGGER.error("Error searching for OldChunk in chunk: {}, {}", chunk.getPos().x, chunk.getPos().z, e);
            }
        });
    }

    private boolean searchChunk(final Chunk chunk) {
        RegistryKey<World> actualDimension = ChunkUtils.getActualDimension();
        if (actualDimension != OVERWORLD && actualDimension != NETHER) {
            return true;
        }
        final MutableBlockPos pos = new MutableBlockPos(0, 0, 0);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 5; y < mc.world.getTopY(); y++) {
                    pos.setPos(x, y, z);
                    BlockState blockState = chunk.getBlockState(pos);
                    if (actualDimension == OVERWORLD) {
                        if (blockState.getBlock() == Blocks.COPPER_ORE
                            || blockState.getBlock() == Blocks.DEEPSLATE_COPPER_ORE
                            || blockState.getBlock() == Blocks.AMETHYST_BLOCK
                            || blockState.getBlock() == Blocks.SMOOTH_BASALT
                            || blockState.getBlock() == Blocks.TUFF
                            || blockState.getBlock() == Blocks.KELP
                            || blockState.getBlock() == Blocks.KELP_PLANT
                            || blockState.getBlock() == Blocks.POINTED_DRIPSTONE
                            || blockState.getBlock() == Blocks.DRIPSTONE_BLOCK
                            || blockState.getBlock() == Blocks.DEEPSLATE
                            || blockState.getBlock() == Blocks.AZALEA
                            || blockState.getBlock() == Blocks.BIG_DRIPLEAF
                            || blockState.getBlock() == Blocks.BIG_DRIPLEAF_STEM
                            || blockState.getBlock() == Blocks.SMALL_DRIPLEAF
                            || blockState.getBlock() == Blocks.MOSS_BLOCK
                            || blockState.getBlock() == Blocks.CAVE_VINES
                            || blockState.getBlock() == Blocks.CAVE_VINES_PLANT
                        ) {
                            return modernChunksCache.addHighlight(chunk.getPos().x, chunk.getPos().z);
                        }
                    } else if (actualDimension == NETHER) {
                        if (blockState.getBlock() == Blocks.ANCIENT_DEBRIS
                            || blockState.getBlock() == Blocks.BLACKSTONE
                            || blockState.getBlock() == Blocks.BASALT
                            || blockState.getBlock() == Blocks.CRIMSON_NYLIUM
                            || blockState.getBlock() == Blocks.WARPED_NYLIUM
                            || blockState.getBlock() == Blocks.NETHER_GOLD_ORE
                            || blockState.getBlock() == Blocks.CHAIN
                        ) {
                            return modernChunksCache.addHighlight(chunk.getPos().x, chunk.getPos().z);
                        }
                    }
                }
            }
        }
        return oldChunksCache.addHighlight(chunk.getPos().x, chunk.getPos().z);
    }

    @Subscribe
    public void onXaeroWorldChangeEvent(final XaeroWorldChangeEvent event) {
        if (XaeroPlusSettingRegistry.oldChunksSaveLoadToDisk.getValue()) {
            if (inUnknownDimension() && oldChunksCache instanceof ChunkHighlightSavingCache) {
                XaeroPlusSettingRegistry.oldChunksSaveLoadToDisk.setValue(false);
                XaeroPlus.LOGGER.warn("Entered unknown dimension with saving cache on, disabling disk saving");
            }
        }
        oldChunksCache.handleWorldChange();
        modernChunksCache.handleWorldChange();
    }

    public boolean inUnknownDimension() {
        final RegistryKey<World> dim = ChunkUtils.getActualDimension();
        return dim != OVERWORLD && dim != NETHER && dim != END;
    }

    @Subscribe
    public void onClientTickEvent(final ClientTickEvent.Post event) {
        oldChunksCache.handleTick();
        modernChunksCache.handleTick();
    }

    @Override
    public void onEnable() {
        oldChunksCache.onEnable();
        modernChunksCache.onEnable();
        searchAllLoadedChunks();
    }

    private void searchAllLoadedChunks() {
        if (mc.world == null || inUnknownDimension()) return;
        final int renderDist = mc.options.getViewDistance().getValue();
        final int xMin = ChunkUtils.getPlayerChunkX() - renderDist;
        final int xMax = ChunkUtils.getPlayerChunkX() + renderDist;
        final int zMin = ChunkUtils.getPlayerChunkZ() - renderDist;
        final int zMax = ChunkUtils.getPlayerChunkZ() + renderDist;
        for (int x = xMin; x <= xMax; x++) {
            for (int z = zMin; z <= zMax; z++) {
                Chunk chunk = mc.world.getChunkManager().getWorldChunk(x, z, false);
                if (chunk instanceof EmptyChunk) continue;
                searchChunkAsync(chunk);
            }
        }
    }

    @Override
    public void onDisable() {
        oldChunksCache.onDisable();
        modernChunksCache.onDisable();
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

    public List<HighlightAtChunkPos> getOldChunksInRegion(
        final int leafRegionX, final int leafRegionZ,
        final int level,
        final RegistryKey<World> dimension) {
        if (inverse)
            return modernChunksCache.getHighlightsInRegion(leafRegionX, leafRegionZ, level, dimension);
        else
            return oldChunksCache.getHighlightsInRegion(leafRegionX, leafRegionZ, level, dimension);
    }

    public boolean isHighlighted(final int chunkPosX, final int chunkPosZ, final RegistryKey<World> dimensionId) {
        if (inverse)
            return isOldChunkInverse(chunkPosX, chunkPosZ, dimensionId);
        else
            return isOldChunk(chunkPosX, chunkPosZ, dimensionId);
    }

    public boolean isOldChunk(final int chunkPosX, final int chunkPosZ, final RegistryKey<World> dimensionId) {
        return oldChunksCache.isHighlighted(chunkPosX, chunkPosZ, dimensionId);
    }

    public boolean isOldChunkInverse(final int chunkPosX, final int chunkPosZ, final RegistryKey<World> dimensionId) {
        return modernChunksCache.isHighlighted(chunkPosX, chunkPosZ, dimensionId);
    }

    public void setInverse(final Boolean b) {
        this.inverse = b;
    }
}
