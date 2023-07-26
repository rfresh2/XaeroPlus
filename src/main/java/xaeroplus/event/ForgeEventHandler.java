package xaeroplus.event;

import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import xaero.common.XaeroMinimapSession;
import xaero.common.minimap.waypoints.render.WaypointsIngameRenderer;
import xaeroplus.XaeroPlus;
import xaeroplus.util.CustomWaypointsIngameRenderer;

public class ForgeEventHandler {
    int errorCount = 0;
    @SubscribeEvent
    public void onRenderWorldLastEvent(final RenderWorldLastEvent event) {
        XaeroMinimapSession minimapSession = XaeroMinimapSession.getCurrentSession();
        if (minimapSession == null) return;
        try {
            WaypointsIngameRenderer waypointsIngameRenderer = minimapSession.getModMain().getInterfaces().getMinimapInterface().getWaypointsIngameRenderer();
            ((CustomWaypointsIngameRenderer) waypointsIngameRenderer).renderWaypointBeacons(minimapSession, event.getContext(), event.getPartialTicks());
        } catch (final Exception e) {
            if (errorCount++ < 2) XaeroPlus.LOGGER.info("Error rendering waypoints", e);
        }
    }
}
