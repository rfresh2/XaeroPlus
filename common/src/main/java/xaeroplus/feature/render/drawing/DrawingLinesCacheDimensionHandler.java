package xaeroplus.feature.render.drawing;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaeroplus.XaeroPlus;
import xaeroplus.feature.render.drawing.db.DrawingDatabase;

import java.util.*;

public class DrawingLinesCacheDimensionHandler {
    private final ResourceKey<Level> dimension;
    private final DrawingDatabase database;
    private final ListeningExecutorService dbExecutor;
    private final Set<ColoredLine> lines = new HashSet<>();
    public final Set<ColoredLine> staleLines = new HashSet<>();
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

    public void addLine(ColoredLine line) {
        if (!mc.isSameThread()) {
            throw new RuntimeException("addLine must be called on the main thread!");
        }
        lines.add(line);
        staleLines.add(line);
    }

    public void removeLine(ColoredLine line) {
        if (!mc.isSameThread()) {
            throw new RuntimeException("removeLine must be called on the main thread!");
        }
        lines.remove(line);
        staleLines.remove(line);
    }

    public Collection<ColoredLine> getLines() {
        return lines;
    }

    protected ListenableFuture<?> loadLines() {
        ListenableFuture<Set<ColoredLine>> loadDataFuture = dbExecutor.submit(this::loadLinesFromDatabase);
        Futures.addCallback(loadDataFuture, new LineDataLoadFutureCallback(), mc);
        return loadDataFuture;
    }

    private Set<ColoredLine> loadLinesFromDatabase() {
        Set<ColoredLine> dataBuf = new HashSet<>();
        database.getLinesInDimension(dimension, (x1, z1, x2, z2, color) -> {
            ColoredLine line = new ColoredLine(x1, z1, x2, z2, color);
            dataBuf.add(line);
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

    public List<ColoredLine> collectStaleLinesToWrite() {
        if (!mc.isSameThread()) {
            throw new RuntimeException("collectStaleHighlightsToWrite must be called on the main thread");
        }
        if (staleLines.isEmpty()) return Collections.emptyList();
        List<ColoredLine> linesToWrite = new ArrayList<>(staleLines.size());
        for (var it = staleLines.iterator(); it.hasNext(); ) {
            ColoredLine line = it.next();
            if (lines.contains(line)) {
                linesToWrite.add(line);
            }
            it.remove();
        }
        return linesToWrite;
    }

    public ListenableFuture<?> writeDataToDatabase(List<ColoredLine> toWrite) {
        try {
            return dbExecutor.submit(() -> database.insertLinesList(toWrite, dimension));
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Failed to submit db write task for {} disk cache dimension: {}", database.databaseName, dimension.location(), e);
            return Futures.immediateFailedFuture(e);
        }
    }

    private final class LineDataLoadFutureCallback implements FutureCallback<Set<ColoredLine>> {
        @Override
        public void onSuccess(Set<ColoredLine> dataBuf) {
            if (!mc.isSameThread()) {
                XaeroPlus.LOGGER.error("LineDataLoadFutureCallback must be called on the main thread");
            }
            if (dataBuf.isEmpty()) return;
            // write new data to local cache
            lines.addAll(dataBuf);
        }

        @Override
        public void onFailure(Throwable t) {
            XaeroPlus.LOGGER.error("Error loading lines {} disk cache dimension: {}",
                database.databaseName,
                dimension.location(),
                t);
        }
    }
}
