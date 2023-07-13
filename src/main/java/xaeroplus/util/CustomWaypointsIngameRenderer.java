package xaeroplus.util;

import net.minecraft.client.util.math.MatrixStack;
import xaero.common.XaeroMinimapSession;

public interface CustomWaypointsIngameRenderer {
    void renderWaypointBeacons(final XaeroMinimapSession minimapSession, final MatrixStack matrixStack, final float tickDelta);
}
