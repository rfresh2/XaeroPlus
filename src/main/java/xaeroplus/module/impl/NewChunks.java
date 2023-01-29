package xaeroplus.module.impl;

import com.collarmc.pounce.Subscribe;
import net.minecraft.network.play.server.SPacketChunkData;
import net.minecraft.util.math.ChunkPos;
import xaeroplus.XaeroPlusSettingRegistry;
import xaeroplus.event.PacketReceivedEvent;
import xaeroplus.module.Module;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static xaeroplus.XaeroPlus.getColor;

@Module.ModuleInfo()
public class NewChunks extends Module {
    private static final ConcurrentHashMap<ChunkPos, Long> chunks = new ConcurrentHashMap<>();
    private static int newChunksColor = getColor(255, 0, 0, 100);
    // somewhat arbitrary number but should be sufficient
    private static final int maxNumber = 5000;

    public NewChunks() {
        this.setEnabled(XaeroPlusSettingRegistry.newChunksEnabledSetting.getBooleanSettingValue());
    }

    @Subscribe
    public void onPacketReceivedEvent(final PacketReceivedEvent event) {
        if (event.packet instanceof SPacketChunkData) {
            final SPacketChunkData chunkData = (SPacketChunkData) event.packet;
            if (!chunkData.isFullChunk()) {
                final ChunkPos chunkPos = new ChunkPos(chunkData.getChunkX(), chunkData.getChunkZ());
                synchronized (chunks) {
                    if (chunks.size() > maxNumber) {
                        // remove oldest 500 chunks
                        final List<ChunkPos> toRemove = chunks.entrySet().stream()
                                .sorted(Map.Entry.comparingByValue())
                                .limit(500)
                                .map(Map.Entry::getKey)
                                .collect(Collectors.toList());
                        toRemove.forEach(chunks::remove);
                    }
                    chunks.put(chunkPos, System.currentTimeMillis());
                }
            }
        }
    }

    public boolean isNewChunk(final ChunkPos chunkPos) {
        return chunks.containsKey(chunkPos);
    }

    public int getNewChunksColor() {
        return newChunksColor;
    }

    public void reset() {
        chunks.clear();
    }

    public void setAlpha(final float a) {
        newChunksColor = getColor(255, 0, 0, (int) a);
    }
}
