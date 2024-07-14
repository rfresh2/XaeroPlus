package xaeroplus.module.impl;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import net.lenni0451.lambdaevents.EventHandler;
import net.minecraft.client.Minecraft;
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
import xaeroplus.event.*;
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

import static xaeroplus.feature.render.ColorHelper.getColor;

public class Portals extends Module {
    private ChunkHighlightCache portalsCache = new ChunkHighlightLocalCache();
    private final Minecraft mc = Minecraft.getInstance();
    private int portalsColor = getColor(0, 255, 0, 100);
    private static final String DATABASE_NAME = "XaeroPlusPortals";
    private final ExecutorService searchExecutor = Executors.newFixedThreadPool(
        1,
        new ThreadFactoryBuilder()
            .setNameFormat("XaeroPlus-Portals-Search-%d")
            .build());

    public void setPortalsCache(final Boolean disk) {
        try {
            final Long2LongMap map = portalsCache.getHighlightsState();
            portalsCache.onDisable();
            if (disk) {
                portalsCache = new ChunkHighlightSavingCache(DATABASE_NAME);
            } else {
                portalsCache = new ChunkHighlightLocalCache();
            }
            if (this.isEnabled()) {
                portalsCache.onEnable();
                if (map != null) portalsCache.loadPreviousState(map);
            }
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error setting portals cache", e);
        }
    }

    @Override
    public void onEnable() {
        Globals.drawManager.registerChunkHighlightProvider(
            this.getClass(),
            new ChunkHighlightProvider(
                this::isPortalChunk,
                this::getPortalsColor
            ));
        portalsCache.onEnable();
        searchAllLoadedChunks();
    }

    @Override
    public void onDisable() {
        portalsCache.onDisable();
        Globals.drawManager.unregister(this.getClass());
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

    @EventHandler
    public void onXaeroWorldChangeEvent(final XaeroWorldChangeEvent event) {
        portalsCache.handleWorldChange();
    }

    @EventHandler
    public void onClientTickEvent(final ClientTickEvent.Post event) {
        portalsCache.handleTick();
    }

    private void findPortalInChunkAsync(final ChunkAccess chunk) {
        findPortalInChunkAsync(chunk, 0);
    }

    private void findPortalInChunkAsync(final ChunkAccess chunk, final int waitMs) {
        if (chunk == null) return;
        searchExecutor.execute(() -> {
            try {
                Thread.sleep(waitMs);
                int iterations = 0;
                while (iterations++ < 3) {
                    if (findPortalInChunk(chunk)) break;
                    // mitigate race condition during world changes hackily
                    Thread.sleep(500);
                }
            } catch (final Throwable e) {
                XaeroPlus.LOGGER.error("Error searching for portal in chunk: {}, {}", chunk.getPos().x, chunk.getPos().z, e);
            }
        });
    }

    private static final ReferenceSet<Block> PORTAL_BLOCKS = new ReferenceOpenHashSet<>();
    static {
        PORTAL_BLOCKS.add(Blocks.END_PORTAL);
        PORTAL_BLOCKS.add(Blocks.END_GATEWAY);
        PORTAL_BLOCKS.add(Blocks.NETHER_PORTAL);
        PORTAL_BLOCKS.add(Blocks.END_PORTAL_FRAME);
    }

    private boolean findPortalInChunk(final ChunkAccess chunk) {
        final boolean chunkHadPortal = portalsCache.isHighlighted(chunk.getPos().x, chunk.getPos().z, ChunkUtils.getActualDimension());
//        var before = System.nanoTime();
        var hasPortal = ChunkScanner.chunkContainsBlocks(chunk, PORTAL_BLOCKS, mc.level.getMinBuildHeight());
//        var after = System.nanoTime();
//        XaeroPlus.LOGGER.info("Scanned chunk {} in {} ns", chunk.getPos(), after - before);
        if (hasPortal) {
            return portalsCache.addHighlight(chunk.getPos().x, chunk.getPos().z);
        } else if (chunkHadPortal) {
            portalsCache.removeHighlight(chunk.getPos().x, chunk.getPos().z);
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
        if (portalsCache.isHighlighted(chunkX, chunkZ, ChunkUtils.getActualDimension())) {
            if (findPortalAtBlockPos(pos)) {
                if (mc.level == null || mc.level.getChunkSource() == null) return;
                LevelChunk worldChunk = mc.level.getChunkSource().getChunk(chunkX, chunkZ, false);
                if (worldChunk != null && !(worldChunk instanceof EmptyLevelChunk)) {
                    // todo: this isn't guaranteed to search _after_ the block update is processed
                    findPortalInChunkAsync(worldChunk, 250);
                }
            }
        } else if (state.getBlock() instanceof NetherPortalBlock || state.getBlock() instanceof EndPortalBlock) {
            portalsCache.addHighlight(chunkX, chunkZ);
        }
    }

    public int getPortalsColor() {
        return portalsColor;
    }

    public void setRgbColor(final int color) {
        portalsColor = ColorHelper.getColorWithAlpha(color, (int) XaeroPlusSettingRegistry.portalsAlphaSetting.getValue());
    }

    public void setAlpha(final float a) {
        portalsColor = ColorHelper.getColorWithAlpha(portalsColor, (int) (a));
    }

    public boolean isPortalChunk(final int chunkPosX, final int chunkPosZ, final ResourceKey<Level> dimensionId) {
        return portalsCache.isHighlighted(chunkPosX, chunkPosZ, dimensionId);
    }
}
