package xaeroplus.mixin.client;

import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.common.minimap.waypoints.WaypointSharingHandler;
import xaero.common.minimap.waypoints.WaypointWorld;
import xaeroplus.settings.XaeroPlusSettingRegistry;

@Mixin(value = WaypointSharingHandler.class, remap = false)
public class MixinWaypointSharingHandler {

    @Inject(method = "shareWaypoint", at = @At("HEAD"), cancellable = true)
    public void shareWaypoint(final Screen parent, final Waypoint w, final WaypointWorld wWorld, final CallbackInfo ci) {
        if (XaeroPlusSettingRegistry.disableWaypointSharing.getValue()) {
            ci.cancel();
        }
    }
}
