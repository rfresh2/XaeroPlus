package xaeroplus.module.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.server.SPacketChunkData;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import xaero.map.WorldMap;
import xaero.map.core.XaeroWorldMapCore;
import xaeroplus.XaeroPlus;
import xaeroplus.event.PacketReceivedEvent;
import xaeroplus.event.XaeroWorldChangeEvent;
import xaeroplus.module.Module;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.util.*;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static xaeroplus.util.ChunkUtils.chunkPosToLong;
import static xaeroplus.util.ChunkUtils.loadHighlightChunksAtRegion;
import static xaeroplus.util.ColorHelper.getColor;

@Module.ModuleInfo()
public class NewChunks extends Module {
    private final Long2LongOpenHashMap chunks = new Long2LongOpenHashMap();
    private int newChunksColor = getColor(255, 0, 0, 100);
    // somewhat arbitrary number but should be sufficient
    private static final int maxNumber = 5000;
//    private Path currentSaveFile;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private int currentDimension = 0;
    private NewChunksDatabase newChunksDatabase;

    // this is an lz4 compressed JSON file
    // I've added v1 as a suffix if we ever need to change file formats and want to convert these without data loss
    private static final String NEWCHUNKS_FILE_NAME = "XaeroPlusNewChunksV1.data";
    private String currentWorldId;

    // todo: handle save load on custom dimension switch
    //   we need to ensure we don't write over the current dimension and that when we switch dimensions we correctly save
    //   ideally we'd also queue up the newchunks in our actual dimension and save them when we switch back

    @SubscribeEvent
    public void onPacketReceivedEvent(final PacketReceivedEvent event) {
        try {
            if (event.packet instanceof SPacketChunkData) {
                final SPacketChunkData chunkData = (SPacketChunkData) event.packet;
                final long chunkPosKey = chunkPosToLong(chunkData.getChunkX(), chunkData.getChunkZ());
                if (!chunkData.isFullChunk()) {
                    // todo: find a way to limit our in-memory NewChunk data usage while having save/load enabled (without data loss)
                    //  some file type that allows us to select/write a subset of data? sqlite?
                    if (!XaeroPlusSettingRegistry.newChunksSaveLoadToDisk.getValue() && chunks.size() > maxNumber) {
                        if (lock.readLock().tryLock(1, TimeUnit.SECONDS)) {
                            // remove oldest 500 chunks
                            final List<Long> toRemove = chunks.entrySet().stream()
                                    .sorted(Map.Entry.comparingByValue())
                                    .limit(500)
                                    .map(Map.Entry::getKey)
                                    .collect(Collectors.toList());
                            lock.readLock().unlock();
                            if (lock.writeLock().tryLock(1, TimeUnit.SECONDS)) {
                                toRemove.forEach(l -> chunks.remove((long) l));
                                lock.writeLock().unlock();
                            }
                        }
                    }
                    if (lock.writeLock().tryLock(1, TimeUnit.SECONDS)) {
                        chunks.put(chunkPosKey, System.currentTimeMillis());
                        lock.writeLock().unlock();
                    }
                } else if (XaeroPlusSettingRegistry.newChunksSeenResetTime.getValue() > 0) {
                    if (lock.readLock().tryLock(1, TimeUnit.SECONDS)) {
                        final long chunkDataSeenTime = chunks.get(chunkPosKey);
                        lock.readLock().unlock();
                        if (chunks.defaultReturnValue() != chunkDataSeenTime) {
                            if (System.currentTimeMillis() - chunkDataSeenTime > XaeroPlusSettingRegistry.newChunksSeenResetTime.getValue() * 1000) {
                                if (lock.writeLock().tryLock(1, TimeUnit.SECONDS)) {
                                    chunks.remove(chunkPosKey);
                                    lock.writeLock().unlock();
                                }
                            }
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
        if (XaeroPlusSettingRegistry.newChunksSaveLoadToDisk.getValue()) {
            try {
                saveChunks();
                reset();
                // check if we are changing dimensions or changing worlds
                // if we are changing worlds we need to create a new database connection
                // if we are changing dimensions, update the current dimension
                if (!Objects.equals(event.worldId, currentWorldId)) {
                    if (event.worldId == null) {
                        // we are disconnecting
                        currentWorldId = null;
                        return;
                    }
                    currentWorldId = event.worldId;
                    newChunksDatabase = new NewChunksDatabase(currentWorldId);
                }
                if (currentDimension != getMCDimension()) {
                    currentDimension = getMCDimension();
                }
//                this.currentSaveFile = getSavePath(event.worldId, event.dimId, event.mwId);
                this.currentDimension = getMCDimension();
                loadChunks();
            } catch (final Exception e) {
                XaeroPlus.LOGGER.error("Error handling Xaero world change in NewChunks", e);
            }
        }
    }

    public int getMCDimension() {
        try {
            return Minecraft.getMinecraft().world.provider.getDimension();
        } catch (final Exception e) {
            return 0;
        }
    }

    @Override
    public void onEnable() {
        if (XaeroPlusSettingRegistry.newChunksSaveLoadToDisk.getValue()) {
            loadChunksWithXaeroState();
        }
    }

    private void loadChunksWithXaeroState() {
        try {
            final String worldId = XaeroWorldMapCore.currentSession.getMapProcessor().getCurrentWorldId();
            final String dimensionId = XaeroWorldMapCore.currentSession.getMapProcessor().getCurrentDimId();
            final String mwId = XaeroWorldMapCore.currentSession.getMapProcessor().getCurrentMWId();
            this.newChunksDatabase = new NewChunksDatabase(worldId);
            this.currentDimension = getMCDimension();
            loadChunks();
        } catch (final Exception e) {
            // expected on game launch
        }
    }

    @Override
    public void onDisable() {
        if (XaeroPlusSettingRegistry.newChunksSaveLoadToDisk.getValue()) {
            try {
                saveChunks();
                reset();
            } catch (final Exception e) {
                // expected on game launch
            }
        }

    }

    // in chunkpos coordinates
    public boolean isNewChunk(final int x, final int z) {
        return isNewChunk(chunkPosToLong(x, z));
    }

    public boolean isNewChunk(final long chunkPos) {
        try {
            if (lock.readLock().tryLock(1, TimeUnit.SECONDS)) {
                boolean containsKey = chunks.containsKey(chunkPos);
                lock.readLock().unlock();
                return containsKey;
            }
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error checking if chunk is new", e);
        }
        return false;
    }

    public int getNewChunksColor() {
        return newChunksColor;
    }

    public void reset() {
        try {
            if (lock.writeLock().tryLock(1, TimeUnit.SECONDS)) {
                chunks.clear();
                lock.writeLock().unlock();
            }
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error resetting NewChunks", e);
        }
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

    private void saveChunks() {
        if (chunks.isEmpty()) {
            return;
        }
        try {
            if (lock.readLock().tryLock(1, TimeUnit.SECONDS)) {
                final List<NewChunkData> chunkData = chunks.entrySet().stream()
                        .map(e -> new NewChunkData(e.getKey(), e.getValue()))
                        .collect(Collectors.toList());
                lock.readLock().unlock();
                writeAsync(chunkData, currentDimension, newChunksDatabase);
            }
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error saving NewChunks", e);
        }
    }

    private void writeAsync(final List<NewChunkData> chunkData, final int currentDimension, final NewChunksDatabase newChunksDatabase) {
        executorService.execute(() -> {
           newChunksDatabase.insertNewChunkList(chunkData, currentDimension);
        });
    }

    private void loadChunks() {
        readAsync(currentDimension, newChunksDatabase);
    }

    private void readAsync(final int currentDimension, final NewChunksDatabase newChunksDatabase) {
        executorService.execute(() -> {
            final List<NewChunkData> chunkData = newChunksDatabase.getNewChunks(currentDimension);
            if (nonNull(chunkData) && !chunkData.isEmpty()) {
                try {
                    if (lock.writeLock().tryLock(1, TimeUnit.SECONDS)) {
                        chunkData.stream().forEach(d -> chunks.put(chunkPosToLong(d.x, d.z), d.foundTime));
                        lock.writeLock().unlock();
                    }
                    XaeroPlus.LOGGER.info("Loaded {} NewChunks from disk", chunkData.size());
                } catch (final Exception e) {
                    XaeroPlus.LOGGER.error("Error loading NewChunks", e);
                }

            }
        });
    }

    public void setSaveLoad(final Boolean b) {
        if (b) {
            loadChunksWithXaeroState();
        } else {
            // currentSaveFile should already be set here
            saveChunks();
        }
    }

    public void setRgbColor(final int color) {
        newChunksColor = ColorHelper.getColorWithAlpha(color, (int) XaeroPlusSettingRegistry.newChunksAlphaSetting.getValue());
    }

    public void setAlpha(final float a) {
        newChunksColor = ColorHelper.getColorWithAlpha(newChunksColor, (int) (a));
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
