package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.IXaeroMinimap;
import xaero.common.misc.Internet;
import xaeroplus.settings.Settings;

@Mixin(value = Internet.class, remap = false)
public class MixinCommonInternet {

    @Inject(method = "checkModVersion", at = @At("HEAD"), cancellable = true, remap = false)
    private static void disableInternetAccessCheck(final IXaeroMinimap modMain, final CallbackInfo ci) {
        if (Settings.REGISTRY.disableXaeroInternetAccess.get()) ci.cancel();
    }
}
