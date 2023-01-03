package xaeroplus;

import net.minecraft.network.Packet;
import net.minecraft.network.play.server.SPacketChunkData;
import net.minecraft.util.math.ChunkPos;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static xaeroplus.XaeroPlus.getColor;

public class NewChunks {

    private static final ConcurrentHashMap<ChunkPos, Long> chunks = new ConcurrentHashMap<>();
    private static int newChunksColor = getColor(255, 0, 0, 100);
    // somewhat arbitrary number but should be sufficient
    private static final int maxNumber = 5000;

    public static void handlePacketEvent(final Packet<?> packet) {
        if (packet instanceof SPacketChunkData
                && (XaeroPlusSettingRegistry.newChunksMinimapSetting.getBooleanSettingValue() || XaeroPlusSettingRegistry.worldMapNewChunksSetting.getBooleanSettingValue())) {
            final SPacketChunkData chunkData = (SPacketChunkData) packet;
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

    public static boolean isNewChunk(final ChunkPos chunkPos) {
        return chunks.containsKey(chunkPos);
    }

    public static int getNewChunksColor() {
        return newChunksColor;
    }

    public static void reset() {
        chunks.clear();
    }

    public static void setAlpha(final float a) {
        newChunksColor = getColor(255, 0, 0, (int) a);
    }
}
