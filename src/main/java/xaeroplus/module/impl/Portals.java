package xaeroplus.module.impl;

import net.minecraft.block.BlockPortal;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.server.SPacketBlockChange;
import net.minecraft.network.play.server.SPacketMultiBlockChange;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import xaeroplus.XaeroPlus;
import xaeroplus.event.ChunkDataEvent;
import xaeroplus.event.PacketReceivedEvent;
import xaeroplus.event.XaeroWorldChangeEvent;
import xaeroplus.module.Module;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.util.ChunkUtils;
import xaeroplus.util.ColorHelper;
import xaeroplus.util.highlights.ChunkHighlightSavingCache;
import xaeroplus.util.highlights.HighlightAtChunkPos;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static xaeroplus.util.ColorHelper.getColor;

@Module.ModuleInfo()
public class Portals extends Module {

    private ChunkHighlightSavingCache portalsCache;
    private final ExecutorService searchExecutor = Executors.newSingleThreadExecutor();
    private final Minecraft mc = Minecraft.getMinecraft();
    private int portalsColor = getColor(0, 255, 0, 100);
    private static final String DATABASE_NAME = "XaeroPlusPortals";

    @Override
    public void onEnable() {
        if (portalsCache == null) {
            portalsCache = new ChunkHighlightSavingCache(DATABASE_NAME);
            portalsCache.onEnable();
            searchAllLoadedChunks();
        }
    }

    @Override
    public void onDisable() {
        if (portalsCache != null) {
            portalsCache.onDisable();
            portalsCache = null;
        }
    }

    public boolean inUnknownDimension() {
        final int dim = ChunkUtils.getActualDimension();
        return dim != 0 && dim != -1 && dim != 1;
    }

    @SubscribeEvent
    public void onChunkData(final ChunkDataEvent event) {
        if (event.isFullChunk()) {
            findPortalInChunkAsync(event.getChunk());
        }
    }
    // 161, 142
    @SubscribeEvent
    public void onPacketReceived(final PacketReceivedEvent event) {
        if (event.packet instanceof SPacketBlockChange) {
            final SPacketBlockChange packet = (SPacketBlockChange) event.packet;
            handleBlockChange(packet.getBlockPosition(), packet.getBlockState());
        } else if (event.packet instanceof SPacketMultiBlockChange) {
            final SPacketMultiBlockChange packet = (SPacketMultiBlockChange) event.packet;
            for (final SPacketMultiBlockChange.BlockUpdateData update : packet.getChangedBlocks()) {
                handleBlockChange(update.getPos(), update.getBlockState());
            }
        }
    }

    @SubscribeEvent
    public void onXaeroWorldChangeEvent(final XaeroWorldChangeEvent event) {
        portalsCache.handleWorldChange();
    }

    @SubscribeEvent
    public void onClientTickEvent(final TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            portalsCache.handleTick();
        }
    }
    private void findPortalInChunkAsync(final Chunk chunk) {
        if (inUnknownDimension()) return;
        searchExecutor.submit(() -> {
            try {
                int iterations = 0;
                while (iterations++ < 3) {
                    if (findPortalInChunk(chunk)) break;
                    // mitigate race condition during world changes hackily
                    Thread.sleep(500);
                }
            } catch (final Throwable e) {
                XaeroPlus.LOGGER.error("Error searching for portal in chunk: {}, {}", chunk.x, chunk.z, e);
            }
        });
    }

    private boolean findPortalInChunk(final Chunk chunk) {
        final boolean chunkHadPortal = portalsCache.isHighlighted(chunk.x, chunk.z);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 256; y++) {
                    IBlockState blockState = chunk.getBlockState(x, y, z);
                    if (blockState.getBlock() instanceof BlockPortal) {
                        return portalsCache.addHighlight(chunk.x, chunk.z);
                    }
                }
            }
        }
        if (chunkHadPortal) {
            portalsCache.removeHighlight(chunk.x, chunk.z);
        }
        return true;
    }

    private void searchAllLoadedChunks() {
        if (mc.world == null || inUnknownDimension()) return;
        final int renderDist = mc.gameSettings.renderDistanceChunks;
        final int xMin = ChunkUtils.getPlayerChunkX() - renderDist;
        final int xMax = ChunkUtils.getPlayerChunkX() + renderDist;
        final int zMin = ChunkUtils.getPlayerChunkZ() - renderDist;
        final int zMax = ChunkUtils.getPlayerChunkZ() + renderDist;
        for (int x = xMin; x <= xMax; x++) {
            for (int z = zMin; z <= zMax; z++) {
                Chunk chunk = mc.world.getChunk(x, z);
                if (!chunk.isLoaded()) continue;
                findPortalInChunkAsync(chunk);
            }
        }
    }

    private void handleBlockChange(final BlockPos pos, final IBlockState state) {
        if (inUnknownDimension()) return;
        if (portalsCache.isHighlighted(ChunkUtils.posToChunkPos(pos.getX()), ChunkUtils.posToChunkPos(pos.getZ()))) {
            if (!(state.getBlock() instanceof BlockPortal)) {
                portalsCache.removeHighlight(ChunkUtils.posToChunkPos(pos.getX()), ChunkUtils.posToChunkPos(pos.getZ()));
            }
        } else if (state.getBlock() instanceof BlockPortal) {
            portalsCache.addHighlight(ChunkUtils.posToChunkPos(pos.getX()), ChunkUtils.posToChunkPos(pos.getZ()));
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
            final int dimension) {
        final ChunkHighlightSavingCache cache = this.portalsCache;
        if (cache == null) return Collections.emptyList();
        else return cache.getHighlightsInRegion(leafRegionX, leafRegionZ, level, dimension);
    }

    public boolean isPortalChunk(final int chunkPosX, final int chunkPosZ, final int dimensionId) {
        final ChunkHighlightSavingCache cache = this.portalsCache;
        if (cache == null) return false;
        else return cache.isHighlighted(chunkPosX, chunkPosZ, dimensionId);
    }
}
