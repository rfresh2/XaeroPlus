package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.waypoint.WaypointSharingHandler;
import xaeroplus.settings.Settings;

@Mixin(value = WaypointSharingHandler.class, remap = false)
public class MixinWaypointSharingHandler {

    @Shadow private Waypoint sharedWaypoint;

    @Inject(method = "shareWaypoint",
        at = @At("HEAD"),
        cancellable = true,
        remap = true) // $REMAP
    public void shareWaypoint(final CallbackInfo ci) {
        if (Settings.REGISTRY.disableWaypointSharing.get()) {
            ci.cancel();
        }
    }

    @Inject(method = "onShareConfirmationResult", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/client/gui/components/ChatComponent;addRecentChat(Ljava/lang/String;)V",
        ordinal = 0
    ), remap = true)
    public void mutateWaypointSharingText(
        final CallbackInfo ci,
        @Local(name = "message") LocalRef<String> containerIdRef
    ) {
        if (Settings.REGISTRY.plainWaypointSharing.get()) {
            containerIdRef.set(sharedWaypoint.getName() + " [" + sharedWaypoint.getX() + ", " + (sharedWaypoint.isYIncluded() ? (sharedWaypoint.getY() + ", ") : "") + sharedWaypoint.getZ() + "]");
        }
    }
}
