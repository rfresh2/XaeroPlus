package xaeroplus.module.impl;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import net.minecraft.network.play.server.SPacketChunkData;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import xaeroplus.XaeroPlus;
import xaeroplus.event.PacketReceivedEvent;
import xaeroplus.event.XaeroWorldChangeEvent;
import xaeroplus.module.Module;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.util.ChunkUtils;
import xaeroplus.util.ColorHelper;
import xaeroplus.util.highlights.HighlightAtChunkPos;
import xaeroplus.util.newchunks.NewChunksCache;
import xaeroplus.util.newchunks.NewChunksLocalCache;
import xaeroplus.util.newchunks.NewChunksSavingCache;

import java.util.List;

import static xaeroplus.util.ColorHelper.getColor;

@Module.ModuleInfo()
public class NewChunks extends Module {
    private NewChunksCache newChunksCache = new NewChunksLocalCache();
    private int newChunksColor = getColor(255, 0, 0, 100);
    private static final String DATABASE_NAME = "XaeroPlusNewChunks";

    public void setNewChunksCache(boolean disk) {
        try {
            Long2LongMap map = newChunksCache.getNewChunksState();
            newChunksCache.onDisable();
            if (disk) {
                newChunksCache = new NewChunksSavingCache(DATABASE_NAME);
            } else {
                newChunksCache = new NewChunksLocalCache();
            }
            if (this.isEnabled()) {
                newChunksCache.onEnable();
                if (map != null) newChunksCache.loadPreviousState(map);
            }
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error closing new chunks cache", e);
        }
    }

    @SubscribeEvent
    public void onPacketReceivedEvent(final PacketReceivedEvent event) {
        if (event.packet instanceof SPacketChunkData) {
            final SPacketChunkData chunkData = (SPacketChunkData) event.packet;
            if (!chunkData.isFullChunk()) {
                newChunksCache.addNewChunk(chunkData.getChunkX(), chunkData.getChunkZ());
            }
        }
    }

    @SubscribeEvent
    public void onXaeroWorldChangeEvent(final XaeroWorldChangeEvent event) {
        if (XaeroPlusSettingRegistry.newChunksSaveLoadToDisk.getValue()) {
            if (inUnknownDimension() && newChunksCache instanceof NewChunksSavingCache) {
                XaeroPlusSettingRegistry.newChunksSaveLoadToDisk.setValue(false);
                XaeroPlus.LOGGER.warn("Entered unknown dimension with saving cache on, disabling disk saving");
            }
        }
        newChunksCache.handleWorldChange();
    }

    @SubscribeEvent
    public void onClientTickEvent(final TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            newChunksCache.handleTick();
        }
    }

    public boolean inUnknownDimension() {
        final int dim = ChunkUtils.getActualDimension();
        return dim != 0 && dim != -1 && dim != 1;
    }

    @Override
    public void onEnable() {
        newChunksCache.onEnable();
    }

    @Override
    public void onDisable() {
        newChunksCache.onDisable();
    }

    public int getNewChunksColor() {
        return newChunksColor;
    }

    public void setRgbColor(final int color) {
        newChunksColor = ColorHelper.getColorWithAlpha(color, (int) XaeroPlusSettingRegistry.newChunksAlphaSetting.getValue());
    }

    public void setAlpha(final float a) {
        newChunksColor = ColorHelper.getColorWithAlpha(newChunksColor, (int) (a));
    }

    public List<HighlightAtChunkPos> getNewChunksInRegion(
            final int leafRegionX, final int leafRegionZ,
            final int level,
            final int dimension) {
        return newChunksCache.getNewChunksInRegion(leafRegionX, leafRegionZ, level, dimension);
    }

    public boolean isNewChunk(final int chunkPosX, final int chunkPosZ, final int dimensionId) {
        return newChunksCache.isNewChunk(chunkPosX, chunkPosZ, dimensionId);
    }
}
