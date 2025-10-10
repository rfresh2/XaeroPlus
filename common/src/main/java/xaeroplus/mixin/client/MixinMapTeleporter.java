package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.teleport.MapTeleporter;
import xaeroplus.XaeroPlus;
import xaeroplus.event.XaeroTeleportAttemptEvent;

@Mixin(value = MapTeleporter.class, remap = false)
public class MixinMapTeleporter {

    @Inject(method = "teleport", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;sendUnsignedCommand(Ljava/lang/String;)Z"
    ), remap = true)
    public void onTeleportAttemptA(CallbackInfo ci) {
        XaeroPlus.EVENT_BUS.call(XaeroTeleportAttemptEvent.INSTANCE);
    }

    @Inject(method = "teleport", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;sendChat(Ljava/lang/String;)V"
    ), remap = true)
    public void onTeleportAttemptB(CallbackInfo ci) {
        XaeroPlus.EVENT_BUS.call(XaeroTeleportAttemptEvent.INSTANCE);
    }
}
