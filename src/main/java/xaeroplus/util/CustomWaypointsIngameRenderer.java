package xaeroplus.util;

import net.minecraft.client.renderer.RenderGlobal;
import xaero.hud.minimap.module.MinimapSession;

public interface CustomWaypointsIngameRenderer {
    void renderWaypointBeacons(final MinimapSession minimapSession, final RenderGlobal renderGlobal, final float partialTicks);
}
