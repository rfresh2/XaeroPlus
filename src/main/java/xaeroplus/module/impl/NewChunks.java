package xaeroplus.module.impl;

import com.collarmc.pounce.Preference;
import com.collarmc.pounce.Subscribe;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.network.play.server.SPacketChunkData;
import net.minecraft.util.math.ChunkPos;
import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorInputStream;
import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorOutputStream;
import xaero.map.WorldMap;
import xaero.map.core.XaeroWorldMapCore;
import xaeroplus.XaeroPlus;
import xaeroplus.XaeroPlusSettingRegistry;
import xaeroplus.event.PacketReceivedEvent;
import xaeroplus.event.XaeroWorldChangeEvent;
import xaeroplus.module.Module;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static xaeroplus.XaeroPlus.getColor;

@Module.ModuleInfo()
public class NewChunks extends Module {
    // todo: we *could* make save/load async
    //      Although it might cause issues as ordering of operations will have to be handled.
    //      Need user feedback and further testing on NewChunk files that get very big

    private static final ConcurrentHashMap<Long, Long> chunks = new ConcurrentHashMap<>();
    private static int newChunksColor = getColor(255, 0, 0, 100);
    // somewhat arbitrary number but should be sufficient
    private static final int maxNumber = 5000;
    private Path currentSaveFile;

    // this is an lz4 compressed JSON file
    // I've added v1 as a suffix if we ever need to change file formats and want to convert these without data loss
    private static final String NEWCHUNKS_FILE_NAME = "XaeroPlusNewChunksV1.data";

    public NewChunks() {
        this.setEnabled(XaeroPlusSettingRegistry.newChunksEnabledSetting.getBooleanSettingValue());
    }

    @Subscribe(value = Preference.CALLER)
    public void onPacketReceivedEvent(final PacketReceivedEvent event) {
        try {
            if (event.packet instanceof SPacketChunkData) {
                final SPacketChunkData chunkData = (SPacketChunkData) event.packet;
                if (!chunkData.isFullChunk()) {
                    final ChunkPos chunkPos = new ChunkPos(chunkData.getChunkX(), chunkData.getChunkZ());
                    synchronized (chunks) {
                        // todo: find a way to limit our in-memory NewChunk data usage while having save/load enabled (without data loss)
                        //  some file type that allows us to select/write a subset of data? sqlite?
                        if (!XaeroPlusSettingRegistry.newChunksSaveLoadToDisk.getBooleanSettingValue() && chunks.size() > maxNumber) {
                            // remove oldest 500 chunks
                            final List<Long> toRemove = chunks.entrySet().stream()
                                    .sorted(Map.Entry.comparingByValue())
                                    .limit(500)
                                    .map(Map.Entry::getKey)
                                    .collect(Collectors.toList());
                            toRemove.forEach(chunks::remove);
                        }
                        chunks.put(chunkPosToLong(chunkPos), System.currentTimeMillis());
                    }
                } else if (XaeroPlusSettingRegistry.newChunksSeenResetTime.getFloatSettingValue() > 0) {
                    final Long chunkKey = chunkPosToLong(new ChunkPos(chunkData.getChunkX(), chunkData.getChunkZ()));
                    final Long chunkDataSeenTime = chunks.get(chunkKey);
                    if (nonNull(chunkDataSeenTime)) {
                        if (System.currentTimeMillis() - chunkDataSeenTime > XaeroPlusSettingRegistry.newChunksSeenResetTime.getFloatSettingValue() * 1000) {
                            chunks.remove(chunkKey);
                        }
                    }
                }
            }
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error handling packet event in NewChunks", e);
        }
    }

    @Subscribe(value = Preference.CALLER)
    public void onXaeroWorldChangeEvent(final XaeroWorldChangeEvent event) {
        if (XaeroPlusSettingRegistry.newChunksSaveLoadToDisk.getBooleanSettingValue()) {
            try {
                synchronized (chunks) {
                    saveChunks(this.currentSaveFile);
                    reset();
                    this.currentSaveFile = getSavePath(event.worldId, event.dimId, event.mwId);
                    loadChunks(this.currentSaveFile);
                }
            } catch (final Exception e) {
                XaeroPlus.LOGGER.error("Error handling Xaero world change in NewChunks", e);
            }
        }
    }

    @Override
    public void onEnable() {
        if (XaeroPlusSettingRegistry.newChunksSaveLoadToDisk.getBooleanSettingValue()) {
            try {
                final String worldId = XaeroWorldMapCore.currentSession.getMapProcessor().getCurrentWorldId();
                final String dimensionId = XaeroWorldMapCore.currentSession.getMapProcessor().getCurrentDimension();
                final String mwId = XaeroWorldMapCore.currentSession.getMapProcessor().getCurrentMWId();
                this.currentSaveFile = getSavePath(worldId, dimensionId, mwId);
                loadChunks(this.currentSaveFile);
            } catch (final Exception e) {
                // expected on game launch
            }
        }
    }

    @Override
    public void onDisable() {
        if (XaeroPlusSettingRegistry.newChunksSaveLoadToDisk.getBooleanSettingValue()) {
            synchronized (chunks) {
                try {
                    saveChunks(this.currentSaveFile);
                    reset();
                } catch (final Exception e) {
                    // expected on game launch
                }
            }
        }
    }

    public static Long chunkPosToLong(final ChunkPos chunkPos) {
        return (long)chunkPos.x & 4294967295L | ((long)chunkPos.z & 4294967295L) << 32;
    }

    public static ChunkPos longToChunkPos(final Long l) {
        return new ChunkPos((int)(l & 4294967295L), (int)(l >> 32 & 4294967295L));
    }

    public boolean isNewChunk(final ChunkPos chunkPos) {
        return chunks.containsKey(chunkPosToLong(chunkPos));
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

    private Path getSavePath(final String worldId, final String dimensionId, final String mwId) {
        if (isNull(worldId) || isNull(dimensionId) || isNull(mwId)) {
            return null;
        }
        final Path mainXaeroWorldMapFolder = WorldMap.saveFolder.toPath();
        try {
            final Path saveFile = mainXaeroWorldMapFolder.resolve(worldId).resolve(dimensionId).resolve(mwId).resolve(NEWCHUNKS_FILE_NAME);
            if (!saveFile.toFile().exists()) {
                saveFile.toFile().createNewFile();
            }
            return saveFile;
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error creating NewChunks save file", e);
        }
        return null;
    }

    public void saveChunks(final Path saveFile) {
        if (isNull(saveFile) || chunks.isEmpty()) {
            return;
        }
        final List<NewChunkData> chunkData = chunks.entrySet().stream()
                .map(e -> new NewChunkData(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
        final Gson gson = new GsonBuilder().create();
        try (Writer writer = new OutputStreamWriter(new FramedLZ4CompressorOutputStream(Files.newOutputStream(saveFile.toFile().toPath())))) {
            gson.toJson(chunkData, writer);
            XaeroPlus.LOGGER.debug("Saved " + chunkData.size() + " NewChunks to disk");
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error saving new chunks to file", e);
        }
    }

    private void loadChunks(final Path saveFile) {
        if (isNull(saveFile)) {
            return;
        }
        if (!Files.exists(saveFile)) {
            return;
        }

        final Gson gson = new GsonBuilder().create();
        final TypeToken<List<NewChunkData>> newChunkDataType = new TypeToken<List<NewChunkData>>() { };
        try (Reader reader = new InputStreamReader(new FramedLZ4CompressorInputStream(Files.newInputStream(saveFile.toFile().toPath())))) {
            final List<NewChunkData> chunkData = gson.fromJson(reader, newChunkDataType.getType());
            if (nonNull(chunkData)) {
                chunkData.stream().forEach(d -> chunks.put(d.chunkPos, d.foundTime));
                XaeroPlus.LOGGER.debug("Loaded " + chunkData.size() + " NewChunks from disk");
            }
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error loading new chunks from file", e);
        }
    }

    // static POJO to help GSON with (de)serialization
    public static class NewChunkData {
        public final Long chunkPos;
        public final Long foundTime;

        public NewChunkData(final Long chunkPos, final long foundTime) {
            this.chunkPos = chunkPos;
            this.foundTime = foundTime;
        }
    }
}
