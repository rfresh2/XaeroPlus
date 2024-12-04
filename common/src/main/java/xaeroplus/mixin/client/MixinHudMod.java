package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.HudMod;
import xaeroplus.XaeroPlus;
import xaeroplus.event.MinimapInitCompletedEvent;

@Mixin(value = HudMod.class, remap = false)
public class MixinHudMod {

    @Inject(method = "loadLater", at = @At("RETURN"))
    public void onClientLoadComplete(final CallbackInfo ci) {
        XaeroPlus.EVENT_BUS.call(MinimapInitCompletedEvent.INSTANCE);
    }
}
