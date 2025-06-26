package xaeroplus.feature.drawing;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import it.unimi.dsi.fastutil.longs.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaeroplus.XaeroPlus;
import xaeroplus.feature.drawing.db.DrawingDatabase;
import xaeroplus.feature.render.text.Text;
import xaeroplus.util.ChunkUtils;

import static xaeroplus.util.ChunkUtils.regionCoordToCoord;

public class DrawingTextCacheDimensionHandler {
    private final ResourceKey<Level> dimension;
    private int windowRegionX = 0;
    private int windowRegionZ = 0;
    private int windowRegionSize = 0;
    private final DrawingDatabase database;
    private final ListeningExecutorService dbExecutor;
    private final Long2ObjectMap<Text> texts = new Long2ObjectOpenHashMap<>();
    public final LongSet staleTexts = new LongOpenHashSet();
    Minecraft mc = Minecraft.getInstance();
    ListenableFuture<?> windowMoveFuture = Futures.immediateVoidFuture();

    public DrawingTextCacheDimensionHandler(
        ResourceKey<Level> dimension,
        DrawingDatabase database,
        ListeningExecutorService dbExecutor
    ) {
        this.dimension = dimension;
        this.database = database;
        this.dbExecutor = dbExecutor;
    }

    public void addText(Text text) {
        if (!mc.isSameThread()) {
            throw new RuntimeException("addText must be called on the main thread");
        }
        long key = ChunkUtils.chunkPosToLong(text.x(), text.z());
        texts.put(key, text);
        staleTexts.add(key);
        writeStaleTextsToDatabase();
    }

    public void removeText(int x, int z) {
        if (!mc.isSameThread()) {
            throw new RuntimeException("removeText must be called on the main thread!");
        }
        long key = ChunkUtils.chunkPosToLong(x, z);
        if (texts.containsKey(key)) {
            texts.remove(key);
            staleTexts.add(key);
            dbExecutor.execute(() -> database.removeText(x, z, dimension));
        }
    }

    public Long2ObjectMap<Text> getTexts() {
        return texts;
    }

    public synchronized void setWindow(int regionX, int regionZ, int regionSize) {
        boolean windowChanged = regionX != windowRegionX || regionZ != windowRegionZ || regionSize != windowRegionSize;
        if (windowChanged
            && !windowMoveFuture.isDone()
            && (regionX != 0 || regionZ != 0 || regionSize != 0) // queue window change if we are clearing it (setting size to 0)
        ) {
            XaeroPlus.LOGGER.debug("Rejecting window move to: [{} {} {}] from: [{} {} {}]", regionX, regionZ, regionSize, windowRegionX, windowRegionZ, windowRegionSize);
            return;
        }
        int prevWindowRegionX = windowRegionX;
        int prevWindowRegionZ = windowRegionZ;
        int prevWindowRegionSize = windowRegionSize;
        this.windowRegionX = regionX;
        this.windowRegionZ = regionZ;
        this.windowRegionSize = regionSize;
        if (windowChanged) {
            try {
                windowMoveFuture = moveWindow0(regionX, regionZ, regionSize, prevWindowRegionX, prevWindowRegionZ, prevWindowRegionSize);
            } catch (final Exception e) {
                XaeroPlus.LOGGER.error("Failed submitting move window task for {} disk cache dimension: {}", database.databaseName, dimension.location(), e);
            }
        }
    }

    private ListenableFuture<?> moveWindow0(final int windowRegionX, final int windowRegionZ, final int windowRegionSize, final int prevWindowRegionX, final int prevWindowRegionZ, final int prevWindowRegionSize) {
        ListenableFuture<Long2ObjectMap<Text>> loadDataFuture = dbExecutor.submit(() -> loadTextsFromDatabase(windowRegionX, windowRegionZ, windowRegionSize, prevWindowRegionX, prevWindowRegionZ, prevWindowRegionSize));
        Futures.addCallback(loadDataFuture, new WindowDataLoadFutureCallback(), mc);
        ListenableFuture<?> removeDataFuture = flushTextsOutsideWindow(windowRegionX, windowRegionZ, windowRegionSize);
        return Futures.allAsList(loadDataFuture, removeDataFuture);
    }

    public Long2ObjectMap<Text> loadTextsFromDatabase(final int windowRegionX, final int windowRegionZ, final int windowRegionSize, final int prevWindowRegionX, final int prevWindowRegionZ, final int prevWindowRegionSize) {
        Long2ObjectMap<Text> texts = new Long2ObjectOpenHashMap<>();
        database.getTextsInWindow(
            dimension,
            windowRegionX - windowRegionSize, windowRegionX + windowRegionSize,
            windowRegionZ - windowRegionSize, windowRegionZ + windowRegionSize,
            (text) -> texts.put(ChunkUtils.chunkPosToLong(text.x(), text.z()), text)
        );
        return texts;
    }

    private ListenableFuture<?> flushTextsOutsideWindow(final int windowRegionX, final int windowRegionZ, final int windowRegionSize) {
        if (!mc.isSameThread()) {
            throw new RuntimeException("flushTextsOutsideWindow must be called on the main thread");
        }
        Long2ObjectMap<Text> dataBuf = new Long2ObjectOpenHashMap<>();
        // write to db and remove data from local cache outside window
        var blockXMin = regionCoordToCoord(windowRegionX - windowRegionSize);
        var blockXMax = regionCoordToCoord(windowRegionX + windowRegionSize);
        var blockZMin = regionCoordToCoord(windowRegionZ - windowRegionSize);
        var blockZMax = regionCoordToCoord(windowRegionZ + windowRegionSize);
        // critical section in mc client tick thread
        // it would have huge benefits if we could optimize this
        // there is no cap on the size of the chunks map
        // so this can require iterating through millions of entries
        for (var it = texts.keySet().longIterator(); it.hasNext(); ) {
            var key = it.nextLong();
            final int blockX = ChunkUtils.longToChunkX(key);
            final int blockZ = ChunkUtils.longToChunkZ(key);
            if (blockX < blockXMin
                || blockX > blockXMax
                || blockZ < blockZMin
                || blockZ > blockZMax) {
                if (staleTexts.contains(key)) {
                    dataBuf.put(key, texts.get(key));
                }
                it.remove();
            }
        }
        return dbExecutor.submit(() -> database.insertTextsList(dataBuf, dimension));
    }

    public Long2ObjectMap<Text> collectStaleTextsToWrite() {
        if (!mc.isSameThread()) {
            throw new RuntimeException("collectStaleTextsToWrite must be called on the main thread");
        }
        if (staleTexts.isEmpty()) return Long2ObjectMaps.emptyMap();
        Long2ObjectMap<Text> textsToWrite = new Long2ObjectOpenHashMap<>(staleTexts.size());
        for (var it = staleTexts.longIterator(); it.hasNext(); ) {
            long key = it.nextLong();
            Text text = texts.get(key);
            if (text != null) {
                textsToWrite.put(key, text);
            }
            it.remove();
        }
        return textsToWrite;
    }

    public ListenableFuture<?> writeDataToDatabase(Long2ObjectMap<Text> toWrite) {
        try {
            return dbExecutor.submit(() -> database.insertTextsList(toWrite, dimension));
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Failed to submit db write task for {} disk cache dimension: {}", database.databaseName, dimension.location(), e);
            return Futures.immediateFailedFuture(e);
        }
    }

    // does not remove from local cache
    public ListenableFuture<?> writeStaleTextsToDatabase() {
        if (!mc.isSameThread()) {
            throw new RuntimeException("writeStaleHighlightsToDatabase must be called on the main thread");
        }
        var toWrite = collectStaleTextsToWrite();
        if (toWrite.isEmpty()) return Futures.immediateVoidFuture();
        return writeDataToDatabase(toWrite);
    }

    private final class WindowDataLoadFutureCallback implements FutureCallback<Long2ObjectMap<Text>> {
        @Override
        public void onSuccess(Long2ObjectMap<Text> dataBuf) {
            if (!mc.isSameThread()) {
                XaeroPlus.LOGGER.error("WindowDataLoadFutureCallback must be called on the main thread");
            }
            if (dataBuf.isEmpty()) return;
            // write new data to local cache
            texts.putAll(dataBuf);
        }

        @Override
        public void onFailure(Throwable t) {
            XaeroPlus.LOGGER.error("Error while moving window for {} disk cache dimension: {}",
                database.databaseName,
                dimension.location(),
                t);
        }
    }

}
