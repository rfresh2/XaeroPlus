package xaeroplus.module.impl;

import net.lenni0451.lambdaevents.EventHandler;
import xaero.map.gui.GuiMap;
import xaeroplus.event.ClientTickEvent;
import xaeroplus.feature.render.async.FrameBudgetQueue;
import xaeroplus.feature.render.async.WorldMapScreenState;
import xaeroplus.module.Module;

/**
 * Moves world map work off the render thread while the world map screen is open:
 * map tile textures and chunk highlight meshes are built on worker threads,
 * and large highlight cache updates are spread over several frames.
 */
public class AsyncWorldMapRendering extends Module {
    @EventHandler
    public void onRenderTick(ClientTickEvent.RenderPre event) {
        WorldMapScreenState.update(mc.screen instanceof GuiMap);
        FrameBudgetQueue.runFrame();
    }

    @Override
    public void onDisable() {
        WorldMapScreenState.update(false);
    }
}
