package xaeroplus.module.impl;

import net.lenni0451.lambdaevents.EventHandler;
import xaero.map.WorldMap;
import xaero.map.core.XaeroWorldMapCore;
import xaeroplus.XaeroPlus;
import xaeroplus.event.ClientTickEvent;
import xaeroplus.module.Module;
import xaeroplus.util.normalizer.RegionReloader;
import xaeroplus.util.normalizer.ReloadSignal;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Polls for normalizer reload signals and schedules hot-reloads of converted regions.
 *
 * <p>Runs every 20 render ticks (~1 second). When the normalizer converts region files
 * and writes a {@code .normalizer_reload} signal file, this handler consumes it and
 * schedules render-thread reloads via {@link RegionReloader}.</p>
 *
 * <p>Regions in other dimensions are deferred — they will naturally load the new file
 * when the player enters that dimension.</p>
 */
public class HotReloadHandler extends Module {

    private static final int POLL_INTERVAL_TICKS = 20;
    private int tickCounter = 0;

    /** Requests that couldn't be processed (region was being written). Retried next poll. */
    private final List<ReloadSignal.ReloadRequest> deferredRequests = new ArrayList<>();

    @EventHandler
    public void onRenderTick(ClientTickEvent.RenderPre event) {
        tickCounter++;
        if (tickCounter < POLL_INTERVAL_TICKS) return;
        tickCounter = 0;

        try {
            poll();
        } catch (Exception e) {
            XaeroPlus.LOGGER.error("[HotReload] Error during poll", e);
        }
    }

    private void poll() {
        // Need an active session to get save folder and current dimension
        var session = XaeroWorldMapCore.currentSession;
        if (session == null) return;
        if (WorldMap.saveFolder == null) return;

        Path saveFolder = WorldMap.saveFolder.toPath();
        String worldId = session.getMapProcessor().getCurrentWorldId();
        if (worldId == null) return;

        // Resolve to the world-specific folder
        Path worldFolder = saveFolder.resolve(worldId);

        // Consume pending reload requests from signal file
        List<ReloadSignal.ReloadRequest> requests = ReloadSignal.consumeRequests(worldFolder);

        // Add any deferred requests from previous polls
        if (!deferredRequests.isEmpty()) {
            requests = new ArrayList<>(requests);
            requests.addAll(deferredRequests);
            deferredRequests.clear();
        }

        if (requests.isEmpty()) return;

        int scheduled = 0;
        int deferred = 0;
        int otherDim = 0;

        for (ReloadSignal.ReloadRequest request : requests) {
            boolean result = RegionReloader.scheduleReload(
                request.dimId(), request.regionX(), request.regionZ()
            );
            if (result) {
                scheduled++;
            } else {
                // Check if it's a different dimension (in which case, don't defer)
                var mapProcessor = session.getMapProcessor();
                var mapWorld = mapProcessor.getMapWorld();
                if (mapWorld != null) {
                    var currentDimKey = mapWorld.getCurrentDimensionId();
                    if (currentDimKey != null) {
                        String currentDimId = mapProcessor.getDimensionName(currentDimKey);
                        if (!request.dimId().equals(currentDimId)) {
                            // Different dimension — will load fresh when entering
                            otherDim++;
                            continue;
                        }
                    }
                }
                // Same dimension but couldn't reload (being written, not loaded, etc.)
                // For "being written" cases, defer to next poll
                deferredRequests.add(request);
                deferred++;
            }
        }

        if (scheduled > 0 || deferred > 0 || otherDim > 0) {
            XaeroPlus.LOGGER.info(
                "[HotReload] Processed {} requests: {} scheduled, {} deferred, {} other-dimension",
                requests.size(), scheduled, deferred, otherDim
            );
        }
    }

    @Override
    protected void onEnable() {
        tickCounter = 0;
        deferredRequests.clear();
    }

    @Override
    protected void onDisable() {
        deferredRequests.clear();
    }
}
