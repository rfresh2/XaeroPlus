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
import xaeroplus.feature.render.ellipse.Ellipse;
import xaeroplus.util.ChunkUtils;

import java.util.HashSet;
import java.util.Set;

public class DrawingEllipseCacheDimensionHandler {
    private final ResourceKey<Level> dimension;
    private int windowRegionX;
    private int windowRegionZ;
    private int windowRegionSize;
    private final DrawingDatabase database;
    private final ListeningExecutorService dbExecutor;
    private final Object2IntMap<Ellipse> ellipses = new Object2IntOpenHashMap<>();
    private final Set<Ellipse> staleEllipses = new HashSet<>();
    private ListenableFuture<?> windowMoveFuture = Futures.immediateVoidFuture();
    private final Minecraft mc = Minecraft.getInstance();

    public DrawingEllipseCacheDimensionHandler(
        final ResourceKey<Level> dimension,
        final DrawingDatabase database,
        final ListeningExecutorService dbExecutor
    ) {
        this.dimension = dimension;
        this.database = database;
        this.dbExecutor = dbExecutor;
    }

    public void addEllipse(final Ellipse ellipse, final int color) {
        checkMainThread("addEllipse");
        ellipses.put(ellipse, color);
        staleEllipses.add(ellipse);
        writeStaleEllipsesToDatabase();
    }

    public void removeEllipse(final Ellipse ellipse) {
        checkMainThread("removeEllipse");
        if (ellipses.containsKey(ellipse)) {
            ellipses.removeInt(ellipse);
            staleEllipses.remove(ellipse);
            dbExecutor.execute(() -> database.removeEllipse(ellipse, dimension));
        }
    }

    public void removeAllEllipses() {
        checkMainThread("removeAllEllipses");
        ellipses.clear();
        staleEllipses.clear();
        dbExecutor.execute(() -> database.removeAllEllipses(dimension));
    }

    public Object2IntMap<Ellipse> getEllipses() {
        return ellipses;
    }

    public synchronized void setWindow(final int regionX, final int regionZ, final int regionSize) {
        var windowChanged = regionX != windowRegionX || regionZ != windowRegionZ || regionSize != windowRegionSize;
        if (windowChanged && !windowMoveFuture.isDone() && (regionX != 0 || regionZ != 0 || regionSize != 0)) {
            XaeroPlus.LOGGER.debug(
                "Rejecting ellipse window move to: [{} {} {}] from: [{} {} {}]",
                regionX, regionZ, regionSize, windowRegionX, windowRegionZ, windowRegionSize
            );
            return;
        }
        windowRegionX = regionX;
        windowRegionZ = regionZ;
        windowRegionSize = regionSize;
        if (windowChanged) {
            try {
                windowMoveFuture = moveWindow(regionX, regionZ, regionSize);
            } catch (final Exception e) {
                XaeroPlus.LOGGER.error("Failed submitting ellipse window move task for {} disk cache dimension: {}", database.databaseName, dimension.location(), e);
            }
        }
    }

    private ListenableFuture<?> moveWindow(final int regionX, final int regionZ, final int regionSize) {
        var loadDataFuture = dbExecutor.submit(() -> loadEllipsesFromDatabase(regionX, regionZ, regionSize));
        Futures.addCallback(loadDataFuture, new EllipseDataLoadFutureCallback(), mc);
        var removeDataFuture = flushEllipsesOutsideWindow(regionX, regionZ, regionSize);
        return Futures.allAsList(loadDataFuture, removeDataFuture);
    }

    private Object2IntMap<Ellipse> loadEllipsesFromDatabase(final int regionX, final int regionZ, final int regionSize) {
        var data = new Object2IntOpenHashMap<Ellipse>();
        var minX = ChunkUtils.regionCoordToCoord(regionX - regionSize);
        var minZ = ChunkUtils.regionCoordToCoord(regionZ - regionSize);
        var maxX = ChunkUtils.regionCoordToCoord(regionX + regionSize);
        var maxZ = ChunkUtils.regionCoordToCoord(regionZ + regionSize);
        database.getEllipsesInDimension(dimension, (centerX, centerZ, radiusX, radiusZ, color) -> {
            var ellipse = new Ellipse(centerX, centerZ, radiusX, radiusZ);
            if (ellipse.intersects(minX, maxX, minZ, maxZ)) {
                data.put(ellipse, color);
            }
        });
        return data;
    }

    private ListenableFuture<?> flushEllipsesOutsideWindow(final int regionX, final int regionZ, final int regionSize) {
        checkMainThread("flushEllipsesOutsideWindow");
        var toWrite = new Object2IntOpenHashMap<Ellipse>();
        var minX = ChunkUtils.regionCoordToCoord(regionX - regionSize);
        var minZ = ChunkUtils.regionCoordToCoord(regionZ - regionSize);
        var maxX = ChunkUtils.regionCoordToCoord(regionX + regionSize);
        var maxZ = ChunkUtils.regionCoordToCoord(regionZ + regionSize);
        for (var iterator = ellipses.keySet().iterator(); iterator.hasNext();) {
            var ellipse = iterator.next();
            if (!ellipse.intersects(minX, maxX, minZ, maxZ)) {
                if (staleEllipses.remove(ellipse)) {
                    toWrite.put(ellipse, ellipses.getInt(ellipse));
                }
                iterator.remove();
            }
        }
        return dbExecutor.submit(() -> database.insertEllipsesList(toWrite, dimension));
    }

    public ListenableFuture<?> writeStaleEllipsesToDatabase() {
        checkMainThread("writeStaleEllipsesToDatabase");
        var toWrite = collectStaleEllipsesToWrite();
        if (toWrite.isEmpty()) return Futures.immediateVoidFuture();
        return dbExecutor.submit(() -> database.insertEllipsesList(toWrite, dimension));
    }

    private Object2IntMap<Ellipse> collectStaleEllipsesToWrite() {
        if (staleEllipses.isEmpty()) return Object2IntMaps.emptyMap();
        var toWrite = new Object2IntOpenHashMap<Ellipse>(staleEllipses.size());
        for (var iterator = staleEllipses.iterator(); iterator.hasNext();) {
            var ellipse = iterator.next();
            if (ellipses.containsKey(ellipse)) {
                toWrite.put(ellipse, ellipses.getInt(ellipse));
            }
            iterator.remove();
        }
        return toWrite;
    }

    private void checkMainThread(final String operation) {
        if (!mc.isSameThread()) {
            throw new RuntimeException(operation + " must be called on the main thread!");
        }
    }

    private final class EllipseDataLoadFutureCallback implements FutureCallback<Object2IntMap<Ellipse>> {

        @Override
        public void onSuccess(final Object2IntMap<Ellipse> data) {
            if (!mc.isSameThread()) {
                XaeroPlus.LOGGER.error("EllipseDataLoadFutureCallback must be called on the main thread");
            }
            ellipses.putAll(data);
        }

        @Override
        public void onFailure(final Throwable throwable) {
            XaeroPlus.LOGGER.error(
                "Error loading ellipses {} disk cache dimension: {}",
                database.databaseName,
                dimension.location(),
                throwable
            );
        }
    }
}
