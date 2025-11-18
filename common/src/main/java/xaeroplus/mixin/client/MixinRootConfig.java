package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.hud.minimap.world.container.config.RootConfig;
import xaeroplus.settings.Settings;

@Mixin(value = RootConfig.class, remap = false)
public class MixinRootConfig {

    @Inject(method = "isTeleportationEnabled", at = @At("HEAD"), cancellable = true)
    public void checkGloballyDisabledTeleportation(CallbackInfoReturnable<Boolean> cir) {
        if (Settings.REGISTRY.disableTeleportation.get()) {
            cir.setReturnValue(false);
        }
    }
}
