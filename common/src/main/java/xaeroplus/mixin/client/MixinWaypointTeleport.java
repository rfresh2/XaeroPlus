package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.hud.minimap.waypoint.WaypointTeleport;
import xaeroplus.XaeroPlus;
import xaeroplus.event.XaeroTeleportAttemptEvent;

@Mixin(value = WaypointTeleport.class, remap = false)
public class MixinWaypointTeleport {

    @Inject(method = "teleportToWaypoint(Lxaero/common/minimap/waypoints/Waypoint;Lxaero/hud/minimap/world/MinimapWorld;Lnet/minecraft/client/gui/screens/Screen;Z)V", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;sendUnsignedCommand(Ljava/lang/String;)Z"
    ), remap = true)
    public void onTeleportAttemptA(CallbackInfo ci) {
        XaeroPlus.EVENT_BUS.call(XaeroTeleportAttemptEvent.INSTANCE);
    }

    @Inject(method = "teleportToWaypoint(Lxaero/common/minimap/waypoints/Waypoint;Lxaero/hud/minimap/world/MinimapWorld;Lnet/minecraft/client/gui/screens/Screen;Z)V", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;sendChat(Ljava/lang/String;)V"
    ), remap = true)
    public void onTeleportAttemptB(CallbackInfo ci) {
        XaeroPlus.EVENT_BUS.call(XaeroTeleportAttemptEvent.INSTANCE);
    }
}
