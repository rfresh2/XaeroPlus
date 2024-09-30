package xaeroplus.module.impl;

import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import net.lenni0451.lambdaevents.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import xaeroplus.Globals;
import xaeroplus.XaeroPlus;
import xaeroplus.event.ChunkBlockUpdateEvent;
import xaeroplus.event.ChunkBlocksUpdateEvent;
import xaeroplus.event.ChunkDataEvent;
import xaeroplus.feature.render.highlights.SavableHighlightCacheInstance;
import xaeroplus.module.Module;
import xaeroplus.settings.Settings;
import xaeroplus.util.ChunkScanner;
import xaeroplus.util.ChunkUtils;
import xaeroplus.util.ColorHelper;

import static xaeroplus.util.ColorHelper.getColor;

public class Portals extends Module {
    private final SavableHighlightCacheInstance portalsCache = new SavableHighlightCacheInstance("XaeroPlusPortals");
    private int portalsColor = getColor(0, 255, 0, 100);
    private static final ReferenceSet<Block> PORTAL_BLOCKS = ReferenceOpenHashSet.of(
        Blocks.END_PORTAL,
        Blocks.END_GATEWAY,
        Blocks.NETHER_PORTAL,
        Blocks.END_PORTAL_FRAME
    );

    public void setDiskCache(final boolean disk) {
        portalsCache.setDiskCache(disk, isEnabled());
    }

    @Override
    public void onEnable() {
        Globals.drawManager.registerChunkHighlightProvider(
            this.getClass(),
            this::getHighlightsSnapshot,
            this::getPortalsColor);
        portalsCache.onEnable();
        searchAllLoadedChunks();
    }

    @Override
    public void onDisable() {
        portalsCache.onDisable();
        Globals.drawManager.unregisterChunkHighlightProvider(this.getClass());
    }

    @EventHandler
    public void onChunkData(final ChunkDataEvent event) {
        findPortalInChunkAsync(event.chunk());
    }

    @EventHandler
    public void onMultiBlockUpdate(final ChunkBlocksUpdateEvent event) {
        event.packet().runUpdates(this::handleBlockChange);
    }

    @EventHandler
    public void onBlockUpdate(final ChunkBlockUpdateEvent event) {
        handleBlockChange(event.packet().getPos(), event.packet().getBlockState());
    }

    private void findPortalInChunkAsync(final ChunkAccess chunk) {
        findPortalInChunkAsync(chunk, 0);
    }

    private void findPortalInChunkAsync(final ChunkAccess chunk, final int waitMs) {
        if (chunk == null) return;
        Globals.moduleExecutorService.get().execute(() -> {
            try {
                Thread.sleep(waitMs);
                int iterations = 0;
                while (iterations++ < 3) {
                    if (findPortalInChunk(chunk)) break;
                    // mitigate race condition during world changes hackily
                    Thread.sleep(500);
                }
            } catch (final Throwable e) {
                XaeroPlus.LOGGER.debug("Error searching for portal in chunk: {}, {}", chunk.getPos().x, chunk.getPos().z, e);
            }
        });
    }

    private boolean findPortalInChunk(final ChunkAccess chunk) {
        final boolean chunkHadPortal = portalsCache.get().isHighlighted(chunk.getPos().x, chunk.getPos().z, ChunkUtils.getActualDimension());
        var hasPortal = ChunkScanner.chunkContainsBlocks(chunk, PORTAL_BLOCKS, mc.level.getMinBuildHeight());
        if (hasPortal) {
            return portalsCache.get().addHighlight(chunk.getPos().x, chunk.getPos().z);
        } else if (chunkHadPortal) {
            portalsCache.get().removeHighlight(chunk.getPos().x, chunk.getPos().z);
        }
        return true;
    }

    private boolean findPortalAtBlockPos(final BlockPos pos) {
        if (mc.level == null) return false;
        int chunkX = ChunkUtils.posToChunkPos(pos.getX());
        int chunkZ = ChunkUtils.posToChunkPos(pos.getZ());
        LevelChunk worldChunk = mc.level.getChunkSource().getChunk(chunkX, chunkZ, false);
        if (worldChunk == null || worldChunk instanceof EmptyLevelChunk) return false;
        BlockState blockState = worldChunk.getBlockState(pos);
        return (blockState.getBlock() instanceof NetherPortalBlock || blockState.getBlock() instanceof EndPortalBlock);
    }

    private void searchAllLoadedChunks() {
        if (mc.level == null) return;
        final int renderDist = mc.options.renderDistance().get();
        final int xMin = ChunkUtils.getPlayerChunkX() - renderDist;
        final int xMax = ChunkUtils.getPlayerChunkX() + renderDist;
        final int zMin = ChunkUtils.getPlayerChunkZ() - renderDist;
        final int zMax = ChunkUtils.getPlayerChunkZ() + renderDist;
        for (int x = xMin; x <= xMax; x++) {
            for (int z = zMin; z <= zMax; z++) {
                ChunkAccess chunk = mc.level.getChunkSource().getChunk(x, z, false);
                if (chunk instanceof EmptyLevelChunk) continue;
                findPortalInChunkAsync(chunk);
            }
        }
    }

    private void handleBlockChange(final BlockPos pos, final BlockState state) {
        int chunkX = ChunkUtils.posToChunkPos(pos.getX());
        int chunkZ = ChunkUtils.posToChunkPos(pos.getZ());
        if (portalsCache.get().isHighlighted(chunkX, chunkZ, ChunkUtils.getActualDimension())) {
            if (findPortalAtBlockPos(pos)) {
                if (mc.level == null || mc.level.getChunkSource() == null) return;
                LevelChunk worldChunk = mc.level.getChunkSource().getChunk(chunkX, chunkZ, false);
                if (worldChunk != null && !(worldChunk instanceof EmptyLevelChunk)) {
                    // todo: this isn't guaranteed to search _after_ the block update is processed
                    findPortalInChunkAsync(worldChunk, 250);
                }
            }
        } else if (state.getBlock() instanceof NetherPortalBlock || state.getBlock() instanceof EndPortalBlock) {
            portalsCache.get().addHighlight(chunkX, chunkZ);
        }
    }

    public int getPortalsColor() {
        return portalsColor;
    }

    public void setRgbColor(final int color) {
        portalsColor = ColorHelper.getColorWithAlpha(color, Settings.REGISTRY.portalsAlphaSetting.getAsInt());
    }

    public void setAlpha(final double a) {
        portalsColor = ColorHelper.getColorWithAlpha(portalsColor, (int) (a));
    }

    public boolean isPortalChunk(final int chunkPosX, final int chunkPosZ, final ResourceKey<Level> dimensionId) {
        return portalsCache.get().isHighlighted(chunkPosX, chunkPosZ, dimensionId);
    }

    public LongList getHighlightsSnapshot(final int windowRegionX, final int windowRegionZ, final int windowRegionSize, final ResourceKey<Level> dimension) {
        return portalsCache.get().getHighlightsSnapshot(dimension);
    }
}
