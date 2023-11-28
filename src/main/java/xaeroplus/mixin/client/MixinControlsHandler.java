package xaeroplus.mixin.client;

import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.controls.ControlsHandler;
import xaeroplus.settings.XaeroPlusBooleanSetting;
import xaeroplus.settings.XaeroPlusSettingsReflectionHax;

@Mixin(value = ControlsHandler.class, remap = false)
public class MixinControlsHandler {

    @Inject(method = "keyDown", at = @At("TAIL"))
    public void keyDown(KeyBinding kb, boolean tickEnd, boolean isRepeat, CallbackInfo ci) {
        if (!tickEnd) {
            XaeroPlusBooleanSetting setting = XaeroPlusSettingsReflectionHax.keybindingMapSupplier.get().get(kb);
            if (setting == null) return;
            setting.setValue(!setting.getValue());
        }
    }
}
