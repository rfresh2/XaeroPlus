package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.waypoint.WaypointTeleport;
import xaero.hud.minimap.world.MinimapWorld;
import xaeroplus.XaeroPlus;
import xaeroplus.event.XaeroTeleportAttemptEvent;
import xaeroplus.settings.Settings;

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

    @WrapOperation(method = "teleportToWaypoint(Lxaero/common/minimap/waypoints/Waypoint;Lxaero/hud/minimap/world/MinimapWorld;Lnet/minecraft/client/gui/screens/Screen;Z)V", at = @At(
        value = "INVOKE",
        target = "Ljava/lang/String;startsWith(Ljava/lang/String;)Z",
        ordinal = 0
    ),
        slice = @Slice(
            from = @At(
                value = "INVOKE",
                target = "Lxaero/common/minimap/waypoints/Waypoint;getYaw()I"
            )
        )
    )
    public boolean crossDimensionWpTeleportCommand(
        final String instance, final String prefix,
        final Operation<Boolean> original,
        @Local(argsOnly = true) Waypoint waypoint,
        @Local(argsOnly = true) MinimapWorld world,
        @Local(name = "fullCommand") LocalRef<String> fullCommandRef,
        @Local(name = "yString") String yString
    ) {
        if (!Settings.REGISTRY.useCustomCrossDimensionWaypointTeleportFormat.get())
            return original.call(instance, prefix);
        if (!fullCommandRef.get().startsWith("/execute in")) // from hardcoded cross-dim format prefix
            return original.call(instance, prefix);
        var template = waypoint.isRotation()
            ? Settings.REGISTRY.crossDimensionWaypointTeleportRotationFormat.get()
            : Settings.REGISTRY.crossDimensionWaypointTeleportFormat.get();
        var dimension = world.getDimId();
        var command = template
            .replace("{x}", Integer.toString(waypoint.getX()))
            .replace("{y}", yString)
            .replace("{z}", Integer.toString(waypoint.getZ()))
            .replace("{d}", dimension.location().toString())
            .replace("{name}", waypoint.getLocalizedName());
        if (waypoint.isRotation()) {
            command = command.replace("{yaw}", Integer.toString(waypoint.getYaw()));
        }
        fullCommandRef.set(command);
        return original.call(fullCommandRef.get(), prefix);
    }
}
