package xaeroplus.mixin;

import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.controls.ControlsHandler;
import xaeroplus.settings.XaeroPlusBooleanSetting;

import java.util.stream.Stream;

import static java.util.Objects.nonNull;
import static xaeroplus.settings.XaeroPlusSettingsReflectionHax.*;

@Mixin(value = ControlsHandler.class, remap = false)
public class MixinControlsHandler {

    @Inject(method = "keyDown", at = @At("TAIL"))
    public void keyDown(KeyBinding kb, boolean tickEnd, boolean isRepeat, CallbackInfo ci) {
        if (!tickEnd) {
            Stream.of(XAERO_PLUS_WORLDMAP_SETTINGS.stream(),
                            XAERO_PLUS_MINIMAP_OVERLAY_SETTINGS.stream(),
                            XAERO_PLUS_MINIMAP_SETTINGS.stream(),
                            XAERO_PLUS_KEYBIND_SETTINGS.stream())
                    .flatMap(x -> x)
                    .filter(xaeroPlusSetting -> xaeroPlusSetting instanceof XaeroPlusBooleanSetting)
                    .map(xaeroPlusSetting -> (XaeroPlusBooleanSetting) xaeroPlusSetting)
                    .filter(s -> nonNull(s.getKeyBinding()))
                    .filter(s -> kb == s.getKeyBinding())
                    .forEach(s -> s.setValue(!s.getValue()));
        }
    }
}
