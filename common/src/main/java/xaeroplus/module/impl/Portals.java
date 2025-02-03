package xaeroplus.module.impl;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import net.lenni0451.lambdaevents.EventHandler;
import net.minecraft.client.multiplayer.ClientLevel;
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
import xaeroplus.Globals;
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
    public final SavableHighlightCacheInstance portalsCache = new SavableHighlightCacheInstance("XaeroPlusPortals");
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
        Globals.drawManager.registry().registerDirectChunkHighlightProvider(
            this.getClass().getName(),
            this::getHighlightsState,
            this::getPortalsColor);
        portalsCache.onEnable();
        searchAllLoadedChunks();
    }

    @Override
    public void onDisable() {
        portalsCache.onDisable();
        Globals.drawManager.registry().unregisterChunkHighlightProvider(this.getClass().getName());
    }

    @EventHandler
    public void onChunkData(final ChunkDataEvent event) {
        findPortalInChunk(event.chunk());
    }

    @EventHandler
    public void onMultiBlockUpdate(final ChunkBlocksUpdateEvent event) {
        switch (event.phase()) {
            case PRE -> event.packet().runUpdates(this::handleBlockChange);
            case POST -> handleMultiBlockChangePost(event);
        }
    }

    // checking for portal removal
    private void handleMultiBlockChangePost(final ChunkBlocksUpdateEvent event) {
        ClientLevel level = mc.level;
        if (level == null) return;
        event.packet().runUpdates(((blockPos, blockState) -> {
            if (!blockState.isAir()) return;
            int chunkX = ChunkUtils.posToChunkPos(blockPos.getX());
            int chunkZ = ChunkUtils.posToChunkPos(blockPos.getZ());
            if (!portalsCache.get().isHighlighted(chunkX, chunkZ, ChunkUtils.getActualDimension())) return;
            ChunkAccess chunk = level.getChunkSource().getChunk(chunkX, chunkZ, false);
            if (chunk instanceof EmptyLevelChunk || chunk == null) return;
            findPortalInChunk(chunk);
        }));
    }

    @EventHandler
    public void onBlockUpdate(final ChunkBlockUpdateEvent event) {
        switch (event.phase()) {
            case PRE -> handleBlockChange(event.packet().getPos(), event.packet().getBlockState());
            case POST -> handleBlockChangePost(event);
        }
    }

    // checking for portal removal
    private void handleBlockChangePost(final ChunkBlockUpdateEvent event) {
        ClientLevel level = mc.level;
        if (level == null) return;
        var blockPos = event.packet().getPos();
        var blockState = event.packet().getBlockState();
        if (!blockState.isAir()) return;
        int chunkX = ChunkUtils.posToChunkPos(blockPos.getX());
        int chunkZ = ChunkUtils.posToChunkPos(blockPos.getZ());
        if (!portalsCache.get().isHighlighted(chunkX, chunkZ, ChunkUtils.getActualDimension())) return;
        ChunkAccess chunk = level.getChunkSource().getChunk(chunkX, chunkZ, false);
        if (chunk instanceof EmptyLevelChunk || chunk == null) return;
        findPortalInChunk(chunk);
    }

    private void findPortalInChunk(final ChunkAccess chunk) {
        final boolean chunkHadPortal = portalsCache.get().isHighlighted(chunk.getPos().x, chunk.getPos().z, ChunkUtils.getActualDimension());
        var hasPortal = ChunkScanner.chunkContainsBlocks(chunk, PORTAL_BLOCKS, mc.level.getMinBuildHeight());
        if (hasPortal) {
            portalsCache.get().addHighlight(chunk.getPos().x, chunk.getPos().z);
        } else if (chunkHadPortal) {
            portalsCache.get().removeHighlight(chunk.getPos().x, chunk.getPos().z);
        }
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
                if (chunk instanceof EmptyLevelChunk || chunk == null) continue;
                findPortalInChunk(chunk);
            }
        }
    }

    private void handleBlockChange(final BlockPos pos, final BlockState state) {
        int chunkX = ChunkUtils.posToChunkPos(pos.getX());
        int chunkZ = ChunkUtils.posToChunkPos(pos.getZ());
        if (!(state.getBlock() instanceof NetherPortalBlock || state.getBlock() instanceof EndPortalBlock)) return;
        portalsCache.get().addHighlight(chunkX, chunkZ);
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

    public Long2LongMap getHighlightsState(final ResourceKey<Level> dimension) {
        return portalsCache.get().getCacheMap(dimension);
    }
}
