package xaeroplus.event;

import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import xaero.common.XaeroMinimapSession;
import xaero.common.minimap.waypoints.render.WaypointsIngameRenderer;
import xaeroplus.util.CustomWaypointsIngameRenderer;

public class ForgeEventHandler {
    @SubscribeEvent
    public void onRenderWorldLastEvent(final RenderWorldLastEvent event) {
        XaeroMinimapSession minimapSession = XaeroMinimapSession.getCurrentSession();
        if (minimapSession == null) return;
        WaypointsIngameRenderer waypointsIngameRenderer = minimapSession.getModMain().getInterfaces().getMinimapInterface().getWaypointsIngameRenderer();
        ((CustomWaypointsIngameRenderer) waypointsIngameRenderer).renderWaypointBeacons(minimapSession, event.getContext(), event.getPartialTicks());
    }
}
