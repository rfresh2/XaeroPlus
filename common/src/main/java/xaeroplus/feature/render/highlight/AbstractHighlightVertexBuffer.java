package xaeroplus.feature.render.highlight;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import net.minecraft.client.renderer.MappableRingBuffer;
import org.jetbrains.annotations.Nullable;
import xaeroplus.Globals;
import xaeroplus.feature.render.DrawContext;
import xaeroplus.feature.render.async.HighlightMeshes;
import xaeroplus.feature.render.async.WorldMapScreenState;

import java.util.concurrent.CompletableFuture;

public abstract class AbstractHighlightVertexBuffer {
    protected boolean stale = true;
    @Nullable protected GpuBuffer vertexBuffer = null;
    protected boolean flipped = false;
    public long lastRefreshed = 0L;
    public int indexCount = 0;
    public MappableRingBuffer uniformBuffer = null;
    // async world map rendering: mesh being built on a worker, and the cell size it was sampled at
    @Nullable private CompletableFuture<HighlightMeshes.Mesh> pendingMesh = null;
    private boolean rebuildAfterPendingMesh = false;
    private int cellShift = 0;

    public boolean needsRefresh(DrawContext ctx) {
        return vertexBuffer == null || vertexBuffer.isClosed() || stale || flipped != ctx.worldmap() || uniformBuffer == null
            || (WorldMapScreenState.isOpen() && cellShift(ctx) != cellShift);
    }

    public void preRender(final DrawContext ctx, final Long2LongMap highlights, final int color) {
        uploadPendingMesh(ctx);
        if (needsRefresh(ctx)) {
            Globals.bypassVertexCountLimit = true;
            refresh(ctx, highlights, color);
            Globals.bypassVertexCountLimit = false;
        }
        if (uniformBuffer == null) {
            uniformBuffer = new MappableRingBuffer(
                () -> "XaeroPlus Highlight Uniform Buffer",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_MAP_WRITE,
                new Std140SizeCalculator()
                    .putMat4f()
                    .putMat4f()
                    .putMat4f()
                    .putVec4()
                    .putVec2()
                    .putVec2()
                    .get()
            );
        }
    }

    public abstract void refresh(DrawContext ctx, Long2LongMap highlights, int color);

    /**
     * While the world map screen is open with async rendering, builds the mesh on a worker thread, sampled down to
     * about one quad per screen pixel, and uploads it in a later {@link #preRender} once ready. The previous mesh keeps
     * being drawn meanwhile. The minimap, and everything after the map screen closes, use the synchronous refresh.
     *
     * @return false if the caller must refresh synchronously
     */
    protected boolean refreshAsync(DrawContext ctx, Long2LongMap highlights, int color, @Nullable MultiColorHighlightColorFunction colorFunction) {
        if (!WorldMapScreenState.isOpen() || !ctx.worldmap()) return false;
        int cellShift = cellShift(ctx);
        boolean changed = stale || flipped != ctx.worldmap() || cellShift != this.cellShift;
        stale = false;
        lastRefreshed = System.currentTimeMillis();
        flipped = ctx.worldmap();
        this.cellShift = cellShift;
        if (highlights.isEmpty() || colorFunction == null && color >>> 24 == 0) {
            close();
            return true;
        }
        if (pendingMesh != null) {
            // Asked again only because the first mesh is not uploaded yet: nothing to rebuild.
            rebuildAfterPendingMesh |= changed;
            return true;
        }
        pendingMesh = HighlightMeshes.build(highlights, flipped, cellShift, colorFunction);
        return true;
    }

    private void uploadPendingMesh(DrawContext ctx) {
        var pending = pendingMesh;
        if (pending == null) return;
        if (!WorldMapScreenState.isOpen() || !ctx.worldmap()) {
            dropPendingMesh(); // map screen closed: the build is cancelled and frees itself
            return;
        }
        if (!pending.isDone()) return;
        pendingMesh = null;
        var mesh = pending.join();
        if (mesh == null) return; // build failed and was logged; the next invalidation retries
        if (vertexBuffer != null) {
            vertexBuffer.close();
            vertexBuffer = null;
        }
        indexCount = mesh.vertexCount() / 4 * 6;
        if (mesh.vertexCount() > 0) {
            vertexBuffer = RenderSystem.getDevice().createBuffer(() -> "Chunk Highlight Buffer", GpuBuffer.USAGE_VERTEX, mesh.data());
        }
        mesh.free();
        if (rebuildAfterPendingMesh) {
            rebuildAfterPendingMesh = false;
            stale = true;
        }
    }

    private void dropPendingMesh() {
        if (pendingMesh == null) return;
        pendingMesh.thenAccept(mesh -> {
            if (mesh != null) mesh.free();
        });
        pendingMesh = null;
        rebuildAfterPendingMesh = false;
    }

    private static int cellShift(DrawContext ctx) {
        return ctx.worldmap() ? HighlightMeshes.cellShift(ctx.fboScale()) : 0;
    }

    public abstract void render(DrawContext ctx, Long2LongMap highlights, int color);

    public void markStale() { stale = true; }

    public void close() {
        dropPendingMesh();
        if (vertexBuffer != null) {
            vertexBuffer.close();
            vertexBuffer = null;
        }
        if (uniformBuffer != null) {
            uniformBuffer.close();
            uniformBuffer = null;
        }
    }
}
