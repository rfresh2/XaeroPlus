package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xaero.lib.client.controls.util.KeyMappingUtils;

@Mixin(value = KeyMappingUtils.class, remap = false)
public class MixinKeyMappingUtils {
    @ModifyExpressionValue(
        method = "getKeyName",
        at = @At(
            value = "CONSTANT",
            args = "stringValue=(unset)"
        )
    )
    private static String unsetKeybindText(String original) {
        return "";
    }
}
