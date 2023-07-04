package xaeroplus.util;

import net.minecraft.client.util.math.MatrixStack;

public interface CustomWaypointsIngameRenderer {
    void renderWaypointBeacons(final MatrixStack matrixStack, final float tickDelta);
}
