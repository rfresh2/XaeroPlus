package xaeroplus.fabric.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.HudMod;
import xaeroplus.fabric.XaeroPlusFabric;

// base class for both Minimap and BetterPVP
@Mixin(value = HudMod.class, remap = false)
public class MixinHudMod {
    @Inject(method = "loadCommon", at = @At("HEAD"))
    public void loadCommon(CallbackInfo ci) {
        XaeroPlusFabric.initialize();
    }
}
