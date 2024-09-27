package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.misc.Internet;
import xaeroplus.settings.Settings;

@Mixin(value = Internet.class, remap = false)
public class MixinWorldMapInternet {

    @Inject(method = "checkModVersion", at = @At("HEAD"), cancellable = true)
    private static void disableInternetAccessCheck(final CallbackInfo ci) {
        if (Settings.REGISTRY.disableXaeroInternetAccess.get()) ci.cancel();
    }
}
