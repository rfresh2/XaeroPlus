package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.BuiltInHudModules;
import xaeroplus.feature.extensions.SyncedWaypoint;
import xaeroplus.settings.Settings;
import xaeroplus.util.ColorHelper;

import java.text.NumberFormat;

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

    @WrapOperation(method = "drawWaypointSlot", at = @At(
        value = "INVOKE",
        target = "Lxaero/common/minimap/waypoints/Waypoint;isThirdParty()Z"
    ), remap = true)
    public boolean drawWaypointDistances(
        final Waypoint w,
        final Operation<Boolean> original,
        @Local(argsOnly = true) GuiGraphicsExtractor guiGraphics,
        @Local(argsOnly = true, index = 3) int x,
        @Local(argsOnly = true, index = 4) int y
    ) {
        if (w == null || !Settings.REGISTRY.waypointsListUIAdditions.get() || !Settings.REGISTRY.waypointsListDistanceColumn.get()) {
            return original.call(w);
        }
        try {
            var renderViewEntity = Minecraft.getInstance().getCameraEntity();
            if (renderViewEntity == null) {
                renderViewEntity = Minecraft.getInstance().player;
            }
            var playerX = renderViewEntity.getX();
            var playerZ = renderViewEntity.getZ();
            var playerY = renderViewEntity.getY();
            // GuiWaypoints.distanceDivided is not necessarily initialized or updated, so calc it ourselves to be safe
            var minimapSession = BuiltInHudModules.MINIMAP.getCurrentSession();
            if (minimapSession == null) return original.call(w);
            var minimapWorld = minimapSession.getWorldManager().getCurrentWorld(minimapSession.getWorldState().getAutoWorldPath());
            var dimensionDivision = minimapSession.getDimensionHelper().getDimensionDivision(minimapWorld);
            var wpX = w.getX(dimensionDivision);
            var wpY = w.getY();
            var wpZ = w.getZ(dimensionDivision);
            var distance = Math.sqrt(Math.pow(playerX - wpX, 2) + Math.pow(playerY - wpY, 2) + Math.pow(playerZ - wpZ, 2));
            var text = NumberFormat.getIntegerInstance().format(distance) + "m";
            var fontRenderer = Minecraft.getInstance().font;
            guiGraphics.text(fontRenderer, text, x + 250, y + 1, ColorHelper.getColor(255, 255, 255, 255));
            return false;
        } catch (Exception e) {
            return original.call(w);
        }
    }
}
