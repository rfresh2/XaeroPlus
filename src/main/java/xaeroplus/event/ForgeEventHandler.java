package xaeroplus.event;

import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import xaero.common.HudMod;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.Minimap;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.render.world.WaypointWorldRenderer;
import xaeroplus.XaeroPlus;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.util.WaypointBeaconRenderer;

public class ForgeEventHandler {
    int errorCount = 0;
    @SubscribeEvent
    public void onRenderWorldLastEvent(final RenderWorldLastEvent event) {
        if (!XaeroPlusSettingRegistry.waypointBeacons.getValue()) return;
        HudMod hudMod = HudMod.INSTANCE;
        if (hudMod == null) return;
        Minimap minimap = hudMod.getMinimap();
        if (minimap == null) return;
        WaypointWorldRenderer waypointsIngameRenderer = minimap.getWaypointWorldRenderer();
        if (waypointsIngameRenderer == null) return;
        MinimapSession minimapSession = BuiltInHudModules.MINIMAP.getCurrentSession();
        if (minimapSession == null) return;
        try {
            WaypointBeaconRenderer.INSTANCE.renderWaypointBeacons(event.getPartialTicks());
        } catch (final Exception e) {
            if (errorCount++ < 2) {
                XaeroPlus.LOGGER.info("Error rendering waypoints", e);
                errorCount = 2;
            }
        }
    }
}
