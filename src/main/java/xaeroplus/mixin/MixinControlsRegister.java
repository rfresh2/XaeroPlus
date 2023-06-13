package xaeroplus.mixin;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.controls.ControlsRegister;
import xaeroplus.settings.XaeroPlusSettingsReflectionHax;

import java.util.List;

@Mixin(value = ControlsRegister.class, remap = false)
public abstract class MixinControlsRegister {
    @Final
    @Shadow
    public List<KeyBinding> keybindings;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void init(CallbackInfo ci) {
        List<KeyBinding> keybinds = XaeroPlusSettingsReflectionHax.getKeybinds();
        this.keybindings.addAll(keybinds);
        keybinds.forEach(KeyBindingHelper::registerKeyBinding);
    }
}
