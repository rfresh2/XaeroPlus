package xaeroplus.feature.extensions;

import com.mojang.blaze3d.vertex.PoseStack;
import xaero.hud.minimap.module.MinimapSession;

public interface CustomWaypointsIngameRenderer {
    void renderWaypointBeacons(final MinimapSession minimapSession, final PoseStack matrixStack, final float tickDelta);
}
