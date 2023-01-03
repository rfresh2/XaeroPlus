package xaeroplus.mixin.client;

import net.minecraft.client.settings.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.settings.ModSettings;
import xaeroplus.XaeroPlusSettingRegistry;

@Mixin(value = ModSettings.class, remap = false)
public class MixinCommonModSettings {
    @Inject(method = "isKeyRepeat", at = @At("RETURN"), cancellable = true)
    public void isKeyRepeat(KeyBinding kb, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(cir.getReturnValue() && XaeroPlusSettingRegistry.getKeybinds().stream().noneMatch(keyBinding -> keyBinding == kb));
    }
}
