package xaeroplus.module.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.minecraft.network.play.server.SPacketChunkData;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorInputStream;
import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorOutputStream;
import xaero.map.WorldMap;
import xaero.map.core.XaeroWorldMapCore;
import xaeroplus.XaeroPlus;
import xaeroplus.event.PacketReceivedEvent;
import xaeroplus.event.XaeroWorldChangeEvent;
import xaeroplus.module.Module;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.util.HighlightAtChunkPos;
import xaeroplus.util.RegionRenderPos;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static xaeroplus.XaeroPlus.getColor;
import static xaeroplus.util.ChunkUtils.chunkPosToLong;
import static xaeroplus.util.ChunkUtils.loadHighlightChunksAtRegion;

@Module.ModuleInfo()
public class NewChunks extends Module {
    private static final Long2LongOpenHashMap chunks = new Long2LongOpenHashMap();
    private static int newChunksColor = getColor(255, 0, 0, 100);
    // somewhat arbitrary number but should be sufficient
    private static final int maxNumber = 5000;
    private Path currentSaveFile;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    // this is an lz4 compressed JSON file
    // I've added v1 as a suffix if we ever need to change file formats and want to convert these without data loss
    private static final String NEWCHUNKS_FILE_NAME = "XaeroPlusNewChunksV1.data";

    @SubscribeEvent
    public void onPacketReceivedEvent(final PacketReceivedEvent event) {
        try {
            if (event.packet instanceof SPacketChunkData) {
                final SPacketChunkData chunkData = (SPacketChunkData) event.packet;
                final long chunkPosKey = chunkPosToLong(chunkData.getChunkX(), chunkData.getChunkZ());
                if (!chunkData.isFullChunk()) {
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
                            toRemove.forEach(l -> chunks.remove((long) l));
                        }
                        chunks.put(chunkPosKey, System.currentTimeMillis());
                    }
                } else if (XaeroPlusSettingRegistry.newChunksSeenResetTime.getFloatSettingValue() > 0) {
                    final long chunkDataSeenTime = chunks.get(chunkPosKey);
                    if (chunks.defaultReturnValue() != chunkDataSeenTime) {
                        if (System.currentTimeMillis() - chunkDataSeenTime > XaeroPlusSettingRegistry.newChunksSeenResetTime.getFloatSettingValue() * 1000) {
                            chunks.remove(chunkPosKey);
                        }
                    }
                }
            }
        } catch (final Exception e) {
            // removing this log as it could possibly spam. we *shouldn't* reach this anyway
//            XaeroPlus.LOGGER.error("Error handling packet event in NewChunks", e);
        }
    }

    @SubscribeEvent
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
            loadChunksWithXaeroState();
        }
    }

    private void loadChunksWithXaeroState() {
        try {
            final String worldId = XaeroWorldMapCore.currentSession.getMapProcessor().getCurrentWorldId();
            final String dimensionId = XaeroWorldMapCore.currentSession.getMapProcessor().getCurrentDimId();
            final String mwId = XaeroWorldMapCore.currentSession.getMapProcessor().getCurrentMWId();
            this.currentSaveFile = getSavePath(worldId, dimensionId, mwId);
            loadChunks(this.currentSaveFile);
        } catch (final Exception e) {
            // expected on game launch
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

    // in chunkpos coordinates
    public boolean isNewChunk(final int x, final int z) {
        return chunks.containsKey(chunkPosToLong(x, z));
    }

    public boolean isNewChunk(final long chunkPos) {
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

    private Path getSavePath(final String worldId, final String dimensionId, final String mwId) {
        if (isNull(worldId) || isNull(dimensionId) || isNull(mwId)) {
            return null;
        }
        final Path mainXaeroWorldMapFolder = WorldMap.saveFolder.toPath();
        try {
            final Path saveFile = mainXaeroWorldMapFolder.resolve(worldId).resolve(dimensionId).resolve(mwId).resolve(NEWCHUNKS_FILE_NAME);
            if (!saveFile.toFile().exists()) {
                XaeroPlus.LOGGER.info("Creating NewChunks save file at {} ", saveFile);
                saveFile.toFile().createNewFile();
            }
            return saveFile;
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error creating NewChunks save file", e);
        }
        return null;
    }

    private void saveChunks(final Path saveFile) {
        if (isNull(saveFile) || chunks.isEmpty()) {
            return;
        }
        final List<NewChunkData> chunkData = chunks.entrySet().stream()
                .map(e -> new NewChunkData(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
        writeAsync(chunkData, saveFile);
    }

    private void writeAsync(final List<NewChunkData> chunkData, final Path saveFile) {
        executorService.execute(() -> {
            final Gson gson = new GsonBuilder().create();
            // todo: we should write to a temp file and then rename replace in case this fails mid-write for whatever reason
            //  also we should do a quick check we aren't writing significantly fewer chunks than what are on disk just in case
            try (Writer writer = new OutputStreamWriter(new FramedLZ4CompressorOutputStream(Files.newOutputStream(saveFile)))) {
                gson.toJson(chunkData, writer);
                XaeroPlus.LOGGER.info("Saved {} NewChunks to disk", chunkData.size());
            } catch (final Exception e) {
                XaeroPlus.LOGGER.error("Error saving new chunks to {}", saveFile,e);
            }
        });
    }

    private void loadChunks(final Path saveFile) {
        if (isNull(saveFile)) {
            return;
        }
        if (!Files.exists(saveFile)) {
            return;
        }
        readAsync(saveFile);
    }

    private void readAsync(final Path saveFile) {
        executorService.execute(() -> {
            final Gson gson = new GsonBuilder().create();
            final TypeToken<List<NewChunkData>> newChunkDataType = new TypeToken<List<NewChunkData>>() { };
            try (Reader reader = new InputStreamReader(new FramedLZ4CompressorInputStream(Files.newInputStream(saveFile.toFile().toPath())))) {
                final List<NewChunkData> chunkData = gson.fromJson(reader, newChunkDataType.getType());
                if (nonNull(chunkData)) {
                    chunkData.stream().forEach(d -> chunks.put((long) d.chunkPos, (long) d.foundTime));
                    XaeroPlus.LOGGER.info("Loaded {} NewChunks from disk", chunkData.size());
                }
            } catch (final Exception e) {
                XaeroPlus.LOGGER.error("Error loading new chunks from file", e);
            }
        });
    }

    public void setSaveLoad(final Boolean b) {
        if (b) {
            loadChunksWithXaeroState();
        } else {
            // currentSaveFile should already be set here
            saveChunks(this.currentSaveFile);
        }
    }

    // static POJO to help GSON with (de)serialization
    private static class NewChunkData {
        public final Long chunkPos;
        public final Long foundTime;

        public NewChunkData(final Long chunkPos, final Long foundTime) {
            this.chunkPos = chunkPos;
            this.foundTime = foundTime;
        }
    }

    /**
     * WorldMap Render caching
     */

    private final Cache<RegionRenderPos, List<HighlightAtChunkPos>> regionRenderCache = CacheBuilder.newBuilder()
            .expireAfterWrite(500, TimeUnit.MILLISECONDS)
            .build();

    public List<HighlightAtChunkPos> getNewChunksInRegion(final int leafRegionX, final int leafRegionZ, final int level) {
        final RegionRenderPos regionRenderPos = new RegionRenderPos(leafRegionX, leafRegionZ, level);
        try {
            return regionRenderCache.get(regionRenderPos, loadHighlightChunksAtRegion(leafRegionX, leafRegionZ, level, this::isNewChunk));
        } catch (ExecutionException e) {
            XaeroPlus.LOGGER.error("Error handling NewChunks region lookup", e);
        }
        return Collections.emptyList();
    }

}
