package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xaero.common.minimap.waypoints.Waypoint;
import xaeroplus.feature.extensions.SyncedWaypoint;
import xaeroplus.settings.Settings;

@Mixin(targets = "xaero.common.gui.GuiWaypoints$List", remap = false)
public abstract class MixinGuiWaypointsList {
    @ModifyExpressionValue(method = "drawWaypointSlot",
        at = @At(
            value = "CONSTANT",
            args = "stringValue=gui.xaero_temporary"))
    public String syncedWaypointTranslationKey(final String original, @Local(argsOnly = true) Waypoint wp) {
        if (!Settings.REGISTRY.waypointsListUIAdditions.get()) return original;
        if (wp instanceof SyncedWaypoint) {
            return "xaeroplus.gui.waypoints.synced_waypoint";
        }
        return original;
    }

//    @Inject(method = "drawWaypointSlot", at = @At(
//        value = "RETURN"
//    ), remap = true)
//    public void drawWaypointDistances(final GuiGraphics guiGraphics, final Waypoint w, final int x, final int y, final CallbackInfo ci) {
//        if (!Settings.REGISTRY.waypointsListUIAdditions.get()) return;
//        if (Settings.REGISTRY.showWaypointDistances.get() && w != null) {
//            Entity renderViewEntity = Minecraft.getInstance().getCameraEntity();
//            final double playerX = renderViewEntity.getX();
//            final double playerZ = renderViewEntity.getZ();
//            final double playerY = renderViewEntity.getY();
//            final double dimensionDivision = GuiWaypoints.distanceDivided;
//            final int wpX = w.getX(dimensionDivision);
//            final int wpY = w.getY();
//            final int wpZ = w.getZ(dimensionDivision);
//            final double distance = Math.sqrt(Math.pow(playerX - wpX, 2) + Math.pow(playerY - wpY, 2) + Math.pow(playerZ - wpZ, 2));
//            final String text = NumberFormat.getIntegerInstance().format(distance) + "m";
//            final Font fontRenderer = Minecraft.getInstance().font;
//            guiGraphics.drawString(fontRenderer, text, x + 250, y + 1, -1);
//        }
//    }
}
