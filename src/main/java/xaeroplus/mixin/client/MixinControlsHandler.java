package xaeroplus.mixin.client;

import net.minecraft.client.settings.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.controls.ControlsHandler;
import xaeroplus.settings.XaeroPlusSetting;
import xaeroplus.settings.XaeroPlusSettingRegistry;

import java.util.stream.Stream;

import static java.util.Objects.nonNull;

@Mixin(value = ControlsHandler.class, remap = false)
public class MixinControlsHandler {

    @Inject(method = "keyDown", at = @At("TAIL"))
    public void keyDown(KeyBinding kb, boolean tickEnd, boolean isRepeat, CallbackInfo ci) {
        if (!tickEnd) {
            Stream.concat(XaeroPlusSettingRegistry.XAERO_PLUS_WORLDMAP_SETTINGS.stream(), XaeroPlusSettingRegistry.XAERO_PLUS_MINIMAP_SETTINGS.stream())
                    .filter(XaeroPlusSetting::isBooleanSetting)
                    .filter(s -> nonNull(s.getKeyBinding()))
                    .filter(s -> kb == s.getKeyBinding())
                    .forEach(s -> s.setBooleanSettingValue(!s.getBooleanSettingValue()));
        }
    }
}
