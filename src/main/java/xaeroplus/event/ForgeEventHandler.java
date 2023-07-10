package xaeroplus.event;

import net.blay09.mods.waystones.util.WaystoneActivatedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Optional;
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

    @SubscribeEvent
    @Optional.Method(modid = "waystones")
    public void onWaystoneActivated(WaystoneActivatedEvent event) {
//        XaeroMinimapSession minimapSession = XaeroMinimapSession.getCurrentSession();
//        if (minimapSession == null) return;
//        final WaypointsManager waypointsManager = minimapSession.getWaypointsManager();
//        final WaypointSet waypointSet = waypointsManager.getWaypoints();
//        if (waypointSet == null) return;
//        final List<Waypoint> waypoints = waypointSet.getList();
//        final double dimDiv = waypointsManager.getDimensionDivision(waypointsManager.getCurrentContainerID());
//        final int x = OptimizedMath.myFloor(event.getPos().getX() * dimDiv);
//        final int z = OptimizedMath.myFloor(event.getPos().getZ() * dimDiv);
//        waypoints.add(new Waypoint(
//                x,
//                event.getPos().getY(),
//                z,
//                event.getWaystoneName() + " [Waystone]",
//                event.getWaystoneName().substring(0, 1).toUpperCase(Locale.ROOT),
//                (int) (Math.random() * COLORS.length),
//                0,
//                false
//        ));
//        try {
//            XaeroMinimap.instance.getSettings().saveWaypoints(waypointsManager.getCurrentWorld());
//        } catch (IOException e) {
//            XaeroPlus.LOGGER.warn("Failed to save waypoints", e);
//        }
//        SupportMods.xaeroMinimap.requestWaypointsRefresh();
    }
}
