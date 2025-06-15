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
import xaeroplus.feature.db.DrawingDatabase;
import xaeroplus.feature.render.line.Line;

import java.util.HashSet;
import java.util.Set;

public class DrawingLinesCacheDimensionHandler {
    private final ResourceKey<Level> dimension;
    private final DrawingDatabase database;
    private final ListeningExecutorService dbExecutor;
    private final Object2IntMap<Line> lines = new Object2IntOpenHashMap<>();
    public final Set<Line> staleLines = new HashSet<>();
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

    protected ListenableFuture<?> loadLines() {
        ListenableFuture<Object2IntMap<Line>> loadDataFuture = dbExecutor.submit(this::loadLinesFromDatabase);
        Futures.addCallback(loadDataFuture, new LineDataLoadFutureCallback(), mc);
        return loadDataFuture;
    }

    private Object2IntMap<Line> loadLinesFromDatabase() {
        Object2IntMap<Line> dataBuf = new Object2IntOpenHashMap<>();
        database.getLinesInDimension(dimension, (x1, z1, x2, z2, color) -> {
            Line line = new Line(x1, z1, x2, z2);
            dataBuf.put(line, color);
        });
        return dataBuf;
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
