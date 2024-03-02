package xaeroplus.feature.extensions;

import com.mojang.blaze3d.vertex.PoseStack;
import xaero.common.XaeroMinimapSession;

public interface CustomWaypointsIngameRenderer {
    void renderWaypointBeacons(final XaeroMinimapSession minimapSession, final PoseStack matrixStack, final float tickDelta);
}
