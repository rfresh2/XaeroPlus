package xaeroplus.feature.drawing;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaeroplus.XaeroPlus;
import xaeroplus.feature.drawing.db.DrawingDatabase;
import xaeroplus.feature.render.line.Line;
import xaeroplus.util.ChunkUtils;

import java.util.HashSet;
import java.util.Set;

public class DrawingLinesCacheDimensionHandler {
    private final ResourceKey<Level> dimension;
    private int windowRegionX = 0;
    private int windowRegionZ = 0;
    private int windowRegionSize = 0;
    private final DrawingDatabase database;
    private final ListeningExecutorService dbExecutor;
    private final Object2IntMap<Line> lines = new Object2IntOpenHashMap<>();
    public final Set<Line> staleLines = new HashSet<>();
    ListenableFuture<?> windowMoveFuture = Futures.immediateVoidFuture();
    Minecraft mc = Minecraft.getInstance();

    public DrawingLinesCacheDimensionHandler(
        ResourceKey<Level> dimension,
        DrawingDatabase database,
        ListeningExecutorService dbExecutor
    ) {
        this.dimension = dimension;
        this.database = database;
        this.dbExecutor = dbExecutor;
    }

    public void addLine(Line line, int color) {
        if (!mc.isSameThread()) {
            throw new RuntimeException("addLine must be called on the main thread!");
        }
        lines.put(line, color);
        staleLines.add(line);
        writeStaleLinesToDatabase();
    }

    public void removeLine(Line line) {
        if (!mc.isSameThread()) {
            throw new RuntimeException("removeLine must be called on the main thread!");
        }
        if (lines.containsKey(line)) {
            lines.removeInt(line);
            staleLines.add(line);
            dbExecutor.execute(() -> database.removeLine(line.x1(), line.z1(), line.x2(), line.z2(), dimension));
        }
    }

    public Object2IntMap<Line> getLines() {
        return lines;
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
                windowMoveFuture = moveWindow0(regionX, regionZ, regionSize);
            } catch (final Exception e) {
                XaeroPlus.LOGGER.error("Failed submitting move window task for {} disk cache dimension: {}", database.databaseName, dimension.location(), e);
            }
        }
    }

    protected ListenableFuture<?> moveWindow0(final int windowRegionX, final int windowRegionZ, final int windowRegionSize) {
        ListenableFuture<Object2IntMap<Line>> loadDataFuture = dbExecutor.submit(() -> loadLinesFromDatabase(windowRegionX, windowRegionZ, windowRegionSize));
        Futures.addCallback(loadDataFuture, new LineDataLoadFutureCallback(), mc);
        ListenableFuture<?> removeDataFuture = flushLinesOutsideWindow(windowRegionX, windowRegionZ, windowRegionSize);
        return Futures.allAsList(loadDataFuture, removeDataFuture);
    }

    private Object2IntMap<Line> loadLinesFromDatabase(final int windowRegionX, final int windowRegionZ, final int windowRegionSize) {
        Object2IntMap<Line> dataBuf = new Object2IntOpenHashMap<>();
        int windowXMin = ChunkUtils.regionCoordToCoord(windowRegionX - windowRegionSize);
        int windowZMin = ChunkUtils.regionCoordToCoord(windowRegionZ - windowRegionSize);
        int windowXMax = ChunkUtils.regionCoordToCoord(windowRegionX + windowRegionSize);
        int windowZMax = ChunkUtils.regionCoordToCoord(windowRegionZ + windowRegionSize);
        database.getLinesInDimension(dimension, (x1, z1, x2, z2, color) -> {
            Line line = new Line(x1, z1, x2, z2);
            if (line.lineClip(windowXMin, windowXMax, windowZMin, windowZMax)) {
                dataBuf.put(line, color);
            }
        });
        return dataBuf;
    }

    private ListenableFuture<?> flushLinesOutsideWindow(final int windowRegionX, final int windowRegionZ, final int windowRegionSize) {
        if (!mc.isSameThread()) {
            throw new RuntimeException("removeChunksOutsideWindow must be called on the main thread");
        }
        Object2IntMap<Line> dataBuf = new Object2IntOpenHashMap<>();
        // write to db and remove data from local cache outside window
        int windowXMin = ChunkUtils.regionCoordToCoord(windowRegionX - windowRegionSize);
        int windowZMin = ChunkUtils.regionCoordToCoord(windowRegionZ - windowRegionSize);
        int windowXMax = ChunkUtils.regionCoordToCoord(windowRegionX + windowRegionSize);
        int windowZMax = ChunkUtils.regionCoordToCoord(windowRegionZ + windowRegionSize);
        for (var it = lines.keySet().iterator(); it.hasNext(); ) {
            var line = it.next();
            if (!line.lineClip(windowXMin, windowXMax, windowZMin, windowZMax)) {
                if (staleLines.contains(line)) {
                    dataBuf.put(line, lines.getInt(line));
                }
                it.remove();
            }
        }
        return dbExecutor.submit(() -> database.insertLinesList(dataBuf, dimension));
    }

    // does not remove from local cache
    public ListenableFuture<?> writeStaleLinesToDatabase() {
        if (!mc.isSameThread()) {
            throw new RuntimeException("writeStaleHighlightsToDatabase must be called on the main thread");
        }
        var toWrite = collectStaleLinesToWrite();
        if (toWrite.isEmpty()) return Futures.immediateVoidFuture();
        return writeDataToDatabase(toWrite);
    }

    public Object2IntMap<Line> collectStaleLinesToWrite() {
        if (!mc.isSameThread()) {
            throw new RuntimeException("collectStaleHighlightsToWrite must be called on the main thread");
        }
        if (staleLines.isEmpty()) return Object2IntMaps.emptyMap();
        Object2IntMap<Line> linesToWrite = new Object2IntOpenHashMap<>(staleLines.size());
        for (var it = staleLines.iterator(); it.hasNext(); ) {
            Line line = it.next();
            var color = lines.getOrDefault(line, Integer.MIN_VALUE);
            if (color != Integer.MIN_VALUE) {
                linesToWrite.put(line, color);
            }
            it.remove();
        }
        return linesToWrite;
    }

    public ListenableFuture<?> writeDataToDatabase(Object2IntMap<Line> toWrite) {
        try {
            return dbExecutor.submit(() -> database.insertLinesList(toWrite, dimension));
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Failed to submit db write task for {} disk cache dimension: {}", database.databaseName, dimension.location(), e);
            return Futures.immediateFailedFuture(e);
        }
    }

    private final class LineDataLoadFutureCallback implements FutureCallback<Object2IntMap<Line>> {
        @Override
        public void onSuccess(Object2IntMap<Line> dataBuf) {
            if (!mc.isSameThread()) {
                XaeroPlus.LOGGER.error("LineDataLoadFutureCallback must be called on the main thread");
            }
            if (dataBuf.isEmpty()) return;
            // write new data to local cache
            lines.putAll(dataBuf);
        }

        @Override
        public void onFailure(Throwable t) {
            XaeroPlus.LOGGER.error("Error loading lines {} disk cache dimension: {}",
                database.databaseName,
                dimension.location(),
                t
            );
        }
    }
}
