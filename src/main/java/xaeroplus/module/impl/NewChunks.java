package xaeroplus.module.impl;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.minecraft.network.play.server.SPacketChunkData;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import xaeroplus.XaeroPlus;
import xaeroplus.event.PacketReceivedEvent;
import xaeroplus.event.XaeroWorldChangeEvent;
import xaeroplus.module.Module;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.util.*;

import java.util.List;

import static xaeroplus.util.ColorHelper.getColor;

@Module.ModuleInfo()
public class NewChunks extends Module {
    // todo:
    //  add back disk saving option and in-memory cache only
    //  async loading/saving
    //  transfer V1 NewChunks to V2 on startup

    private NewChunksCache newChunksCache = new NewChunksLocalCache();
    private int newChunksColor = getColor(255, 0, 0, 100);

    // this is an lz4 compressed JSON file
    // I've added v1 as a suffix if we ever need to change file formats and want to convert these without data loss
    // todo: convert these into the sqlite db on startup, then move file to a backup location
    private static final String NEWCHUNKS_FILE_NAME = "XaeroPlusNewChunksV1.data";

    public void setNewChunksCache(boolean disk) {
        try {
            Long2LongOpenHashMap map = newChunksCache.getNewChunksState();
            newChunksCache.onDisable();
            if (disk) {
                newChunksCache = new NewChunksSavingCache();
            } else {
                newChunksCache = new NewChunksLocalCache();
            }
            newChunksCache.loadPreviousState(map);
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error closing new chunks cache", e);
        }
    }


    // todo: handle save load on custom dimension switch
    //   we need to ensure we don't write over the current dimension and that when we switch dimensions we correctly save
    //   ideally we'd also queue up the newchunks in our actual dimension and save them when we switch back

    @SubscribeEvent
    public void onPacketReceivedEvent(final PacketReceivedEvent event) {
        try {
            if (event.packet instanceof SPacketChunkData) {
                final SPacketChunkData chunkData = (SPacketChunkData) event.packet;
                if (!chunkData.isFullChunk()) {
                    newChunksCache.addNewChunk(chunkData.getChunkX(), chunkData.getChunkZ());
                }
            }
        } catch (final Exception e) {
            // removing this log as it could possibly spam. we *shouldn't* reach this anyway
//            XaeroPlus.LOGGER.error("Error handling packet event in NewChunks", e);
        }
    }

    @SubscribeEvent
    public void onXaeroWorldChangeEvent(final XaeroWorldChangeEvent event) {
        newChunksCache.handleWorldChange();
    }

    @SubscribeEvent
    public void onClientTickEvent(final TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            newChunksCache.handleTick();
        }
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
