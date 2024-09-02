package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.common.minimap.waypoints.WaypointSharingHandler;
import xaero.hud.minimap.world.MinimapWorld;
import xaeroplus.settings.XaeroPlusSettingRegistry;

@Mixin(value = WaypointSharingHandler.class, remap = false)
public class MixinWaypointSharingHandler {

    @Shadow private Waypoint w;

    @Inject(method = "shareWaypoint(Lnet/minecraft/client/gui/screens/Screen;Lxaero/common/minimap/waypoints/Waypoint;Lxaero/hud/minimap/world/MinimapWorld;)V",
        at = @At("HEAD"),
        cancellable = true,
        remap = true) // $REMAP
    public void shareWaypoint(final Screen parent, final Waypoint w, final MinimapWorld wWorld, final CallbackInfo ci) {
        if (XaeroPlusSettingRegistry.disableWaypointSharing.getValue()) {
            ci.cancel();
        }
    }

    @Inject(method = "confirmResult", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/client/Minecraft;getInstance()Lnet/minecraft/client/Minecraft;",
        ordinal = 0),
        slice = @Slice(
            from = @At(
                value = "INVOKE",
                target = "Lxaero/common/minimap/waypoints/WaypointSharingHandler;removeFormatting(Ljava/lang/String;)Ljava/lang/String;",
                ordinal = 0
            )
        ),
        remap = true)
    public void mutateWaypointSharingText(final boolean confirm, final CallbackInfo ci,
                                          @Local(name = "message") LocalRef<String> containerIdRef) {
        if (XaeroPlusSettingRegistry.plainWaypointSharing.getValue()) {
            containerIdRef.set(w.getName() + " [" + w.getX() + ", " + (w.isYIncluded() ? (w.getY() + ", ") : "") + w.getZ() + "]");
        }
    }
}
