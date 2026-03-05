package xaeroplus.util.normalizer;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaero.map.MapProcessor;
import xaero.map.core.XaeroWorldMapCore;
import xaero.map.region.MapRegion;
import xaero.map.region.MapTileChunk;
import xaero.map.region.texture.LeafRegionTexture;
import xaeroplus.XaeroPlus;
import xaeroplus.mixin.client.AccessorLeveledRegion;
import xaeroplus.mixin.client.AccessorMapRegion;
import xaeroplus.mixin.client.AccessorMapTileChunk;

import java.util.List;

/**
 * Forces Xaero's World Map to reload specific regions from disk without requiring a relog.
 *
 * <p>Implements Strategy B from the hot-reload spec: uses mixin accessor interfaces to
 * expose Xaero's internal load state fields. Does not modify Xaero's source code.</p>
 *
 * <h3>Reload Sequence (render thread):</h3>
 * <ol>
 *   <li>Acquire {@code writerThreadPauseSync} on the region</li>
 *   <li>For each MapTileChunk (8x8):
 *     <ul>
 *       <li>Delete GL textures and buffers via LeafRegionTexture</li>
 *       <li>Reset chunk loadState to 0</li>
 *       <li>Clear toUpdateBuffers flag</li>
 *     </ul>
 *   </li>
 *   <li>Reset region loadState to 0</li>
 * </ol>
 * <p>Xaero's MapProcessor detects loadState 0 on the next tick and queues a fresh disk read.</p>
 *
 * <h3>Threading:</h3>
 * <p>All GL operations MUST run on the render thread. {@link #scheduleReload} wraps the
 * operation in {@link Minecraft#execute(Runnable)} to ensure this.</p>
 */
public final class RegionReloader {

    private RegionReloader() {}

    /**
     * Schedule a region reload on the render thread.
     *
     * <p>If the region is not currently loaded in memory, this is a no-op —
     * Xaero will load the new file from disk when the player scrolls to that area.</p>
     *
     * @param dimId   the dimension identifier (e.g. "dim%0")
     * @param regionX the region X coordinate
     * @param regionZ the region Z coordinate
     * @return true if the reload was scheduled, false if session/region is unavailable
     */
    public static boolean scheduleReload(String dimId, int regionX, int regionZ) {
        var session = XaeroWorldMapCore.currentSession;
        if (session == null) return false;
        MapProcessor mapProcessor = session.getMapProcessor();
        if (mapProcessor == null) return false;

        // Check the current dimension matches. If not, the region file will be
        // naturally loaded from its new version when the player enters that dimension.
        var mapWorld = mapProcessor.getMapWorld();
        if (mapWorld == null) return false;
        var currentDimKey = mapWorld.getCurrentDimensionId();
        if (currentDimKey == null) return false;

        // Convert the current dimension ResourceKey to the dimension ID string
        // that matches the format used in save paths (e.g. "dim%0", "dim%-1")
        String currentDimId = mapProcessor.getDimensionName(currentDimKey);
        if (currentDimId == null || !currentDimId.equals(dimId)) {
            // Different dimension — region will load fresh when player enters that dim
            return false;
        }

        // Look up the loaded region. create=false so we don't create an empty one
        int caveLayer = mapProcessor.getCurrentCaveLayer();
        MapRegion region = mapProcessor.getLeafMapRegion(caveLayer, regionX, regionZ, false);
        if (region == null) {
            // Not loaded — Xaero will read the new file when it's needed
            return false;
        }

        // Check write guard — don't reload while Xaero is writing to disk
        AccessorLeveledRegion leveledAccessor = (AccessorLeveledRegion) region;
        if (leveledAccessor.invokeIsBeingWritten()) {
            XaeroPlus.LOGGER.debug("[HotReload] Region ({},{}) is being written, deferring reload", regionX, regionZ);
            return false;
        }

        // Schedule the actual reload on the render thread
        Minecraft.getInstance().execute(() -> forceReload(region, regionX, regionZ));
        return true;
    }

    /**
     * Force-reload a region. MUST be called on the render thread.
     *
     * <p>Resets the region and all its chunks to loadState 0, causing Xaero
     * to re-read the region file from disk on the next processing tick.</p>
     */
    private static void forceReload(MapRegion region, int regionX, int regionZ) {
        try {
            AccessorMapRegion regionAccessor = (AccessorMapRegion) region;
            AccessorLeveledRegion leveledAccessor = (AccessorLeveledRegion) region;
            Object pauseSync = regionAccessor.getWriterThreadPauseSync();

            synchronized (pauseSync) {
                // Iterate all 8x8 = 64 MapTileChunks in the region
                for (int cx = 0; cx < 8; cx++) {
                    for (int cz = 0; cz < 8; cz++) {
                        MapTileChunk chunk = region.getChunk(cx, cz);
                        if (chunk == null) continue;

                        AccessorMapTileChunk chunkAccessor = (AccessorMapTileChunk) chunk;

                        // Delete GL textures and buffers (safe on render thread)
                        LeafRegionTexture leafTexture = chunkAccessor.getLeafTexture();
                        if (leafTexture != null) {
                            leafTexture.deleteTexturesAndBuffers();
                        }

                        // Reset chunk load state to 0 (not loaded)
                        chunkAccessor.setLoadState((byte) 0);
                        chunkAccessor.setToUpdateBuffers(false);
                    }
                }

                // Reset region load state to 0
                leveledAccessor.setLoadState((byte) 0);
            }

            XaeroPlus.LOGGER.info("[HotReload] Region ({},{}) reset to loadState 0, will reload from disk",
                regionX, regionZ);
        } catch (Exception e) {
            XaeroPlus.LOGGER.error("[HotReload] Failed to force-reload region ({},{})", regionX, regionZ, e);
        }
    }

    /**
     * Schedule reloads for a batch of regions.
     *
     * @param requests list of reload requests
     * @return number of successfully scheduled reloads
     */
    public static int scheduleReloads(List<ReloadSignal.ReloadRequest> requests) {
        int scheduled = 0;
        for (ReloadSignal.ReloadRequest request : requests) {
            if (scheduleReload(request.dimId(), request.regionX(), request.regionZ())) {
                scheduled++;
            }
        }
        return scheduled;
    }
}
