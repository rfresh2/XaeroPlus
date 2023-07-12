package xaeroplus.util;

import net.minecraft.client.renderer.RenderGlobal;
import xaero.common.XaeroMinimapSession;

public interface CustomWaypointsIngameRenderer {
    void renderWaypointBeacons(final XaeroMinimapSession minimapSession, final RenderGlobal renderGlobal, final float partialTicks);
}
