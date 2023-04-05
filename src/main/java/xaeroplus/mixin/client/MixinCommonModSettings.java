package xaeroplus.mixin.client;

import net.minecraft.client.settings.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.IXaeroMinimap;
import xaero.common.settings.ModSettings;
import xaeroplus.settings.XaeroPlusSettingsReflectionHax;

@Mixin(value = ModSettings.class, remap = false)
public class MixinCommonModSettings {
    @Shadow public int caveMaps;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void init(final IXaeroMinimap modMain, final CallbackInfo ci) {
        // disable cave mode by default
        this.caveMaps = 0;
    }

    @Inject(method = "isKeyRepeat", at = @At("RETURN"), cancellable = true)
    public void isKeyRepeat(KeyBinding kb, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(cir.getReturnValue() && XaeroPlusSettingsReflectionHax.getKeybinds().stream().noneMatch(keyBinding -> keyBinding == kb));
    }
}
