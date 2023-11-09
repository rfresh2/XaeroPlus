package xaeroplus.module.impl;

import com.collarmc.pounce.Subscribe;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import net.minecraft.block.BlockState;
import net.minecraft.block.EndPortalBlock;
import net.minecraft.block.NetherPortalBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.WorldChunk;
import xaeroplus.XaeroPlus;
import xaeroplus.event.ChunkDataEvent;
import xaeroplus.event.ClientTickEvent;
import xaeroplus.event.PacketReceivedEvent;
import xaeroplus.event.XaeroWorldChangeEvent;
import xaeroplus.module.Module;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.util.*;
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
public class Portals extends Module {
    private ChunkHighlightCache portalsCache = new ChunkHighlightLocalCache();
    private final MinecraftClient mc = MinecraftClient.getInstance();
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
        Shared.drawManager.registerChunkHighlightDrawFeature(
            this.getClass(),
            new DrawManager.ChunkHighlightDrawFeature(
                this::isEnabled,
                this::isPortalChunk,
                this::getPortalsInRegion,
                this::getPortalsColor
            ));
        portalsCache.onEnable();
        searchAllLoadedChunks();
    }

    @Override
    public void onDisable() {
        portalsCache.onDisable();
    }

    public boolean inUnknownDimension() {
        final RegistryKey<World> dim = ChunkUtils.getActualDimension();
        return dim != OVERWORLD && dim != NETHER && dim != END;
    }

    @Subscribe
    public void onChunkData(final ChunkDataEvent event) {
        findPortalInChunkAsync(event.chunk());
    }

    @Subscribe
    public void onPacketReceived(final PacketReceivedEvent event) {
        if (event.packet() instanceof BlockUpdateS2CPacket) {
            final BlockUpdateS2CPacket packet = (BlockUpdateS2CPacket) event.packet();
            handleBlockChange(packet.getPos(), packet.getState());
        } else if (event.packet() instanceof ChunkDeltaUpdateS2CPacket) {
            final ChunkDeltaUpdateS2CPacket packet = (ChunkDeltaUpdateS2CPacket) event.packet();
            packet.visitUpdates(this::handleBlockChange);
        }
    }

    @Subscribe
    public void onXaeroWorldChangeEvent(final XaeroWorldChangeEvent event) {
        if (XaeroPlusSettingRegistry.portalsSaveLoadToDisk.getValue()) {
            if (inUnknownDimension() && portalsCache instanceof ChunkHighlightSavingCache) {
                XaeroPlusSettingRegistry.portalsSaveLoadToDisk.setValue(false);
                XaeroPlus.LOGGER.warn("Entered unknown dimension with saving cache on, disabling disk saving");
            }
        }
        portalsCache.handleWorldChange();
    }

    @Subscribe
    public void onClientTickEvent(final ClientTickEvent.Post event) {
        portalsCache.handleTick();
    }

    private void findPortalInChunkAsync(final Chunk chunk) {
        findPortalInChunkAsync(chunk, 0);
    }

    private void findPortalInChunkAsync(final Chunk chunk, final int waitMs) {
        if (inUnknownDimension()) return;
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

    private boolean findPortalInChunk(final Chunk chunk) {
        final boolean chunkHadPortal = portalsCache.isHighlighted(chunk.getPos().x, chunk.getPos().z, ChunkUtils.getActualDimension());
        final MutableBlockPos pos = new MutableBlockPos(0, 0, 0);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = mc.world.getBottomY(); y < mc.world.getTopY(); y++) {
                    pos.setPos(x, y, z);
                    BlockState blockState = chunk.getBlockState(pos);
                    if (blockState.getBlock() instanceof NetherPortalBlock || blockState.getBlock() instanceof EndPortalBlock) {
                        return portalsCache.addHighlight(chunk.getPos().x, chunk.getPos().z);
                    }
                }
            }
        }
        if (chunkHadPortal) {
            portalsCache.removeHighlight(chunk.getPos().x, chunk.getPos().z);
        }
        return true;
    }

    private boolean findPortalAtBlockPos(final BlockPos pos) {
        if (mc.world == null || inUnknownDimension()) return false;
        int chunkX = ChunkUtils.posToChunkPos(pos.getX());
        int chunkZ = ChunkUtils.posToChunkPos(pos.getZ());
        WorldChunk worldChunk = mc.world.getChunkManager().getWorldChunk(chunkX, chunkZ, false);
        if (worldChunk == null || worldChunk instanceof EmptyChunk) return false;
        BlockState blockState = worldChunk.getBlockState(pos);
        return (blockState.getBlock() instanceof NetherPortalBlock || blockState.getBlock() instanceof EndPortalBlock);
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
                findPortalInChunkAsync(chunk);
            }
        }
    }

    private void handleBlockChange(final BlockPos pos, final BlockState state) {
        if (inUnknownDimension()) return;
        int chunkX = ChunkUtils.posToChunkPos(pos.getX());
        int chunkZ = ChunkUtils.posToChunkPos(pos.getZ());
        if (portalsCache.isHighlighted(chunkX, chunkZ, ChunkUtils.getActualDimension())) {
            if (findPortalAtBlockPos(pos)) {
                if (mc.world == null || mc.world.getChunkManager() == null) return;
                WorldChunk worldChunk = mc.world.getChunkManager().getWorldChunk(chunkX, chunkZ, false);
                if (worldChunk != null && !(worldChunk instanceof EmptyChunk)) {
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

    public List<HighlightAtChunkPos> getPortalsInRegion(
            final int leafRegionX, final int leafRegionZ,
            final int level,
            final RegistryKey<World> dimension) {
        return portalsCache.getHighlightsInRegion(leafRegionX, leafRegionZ, level, dimension);
    }

    public boolean isPortalChunk(final int chunkPosX, final int chunkPosZ, final RegistryKey<World> dimensionId) {
        return portalsCache.isHighlighted(chunkPosX, chunkPosZ, dimensionId);
    }
}
