package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import xaero.map.misc.Misc;

@Mixin(value = Misc.class, remap = false)
public class MixinMisc {
    @ModifyConstant(
        method = "getKeyName",
        constant = @Constant(stringValue = "(unset)")
    )
    private static String unsetKeybindText(String original) {
        return "";
    }
}
