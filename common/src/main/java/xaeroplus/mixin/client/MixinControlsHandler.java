package xaeroplus.mixin.client;

import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.controls.ControlsHandler;
import xaeroplus.settings.BooleanSetting;
import xaeroplus.settings.Settings;

@Mixin(value = ControlsHandler.class, remap = false)
public class MixinControlsHandler {

    @Inject(method = "keyDown", at = @At("RETURN"))
    public void keyDown(KeyMapping kb, boolean tickEnd, boolean isRepeat, CallbackInfo ci) {
        if (!tickEnd) {
            BooleanSetting setting = Settings.REGISTRY.getKeybindingSetting(kb);
            if (setting == null) return;
            setting.setValue(!setting.get());
        }
    }
}
