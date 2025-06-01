package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import xaero.map.misc.Misc;

import java.util.concurrent.ThreadLocalRandom;

@Mixin(value = Misc.class, remap = false)
public class MixinWorldMapMisc {
    @ModifyConstant(
        method = "getKeyName",
        constant = @Constant(stringValue = "(unset)")
    )
    private static String unsetKeybindText(String original) {
        return "";
    }

    @ModifyExpressionValue(method = "quickFileBackupMove", at = @At(
        value = "CONSTANT",
        args = "intValue=0",
        ordinal = 0
    ))
    private static int randomizeBackupNumber(int original) {
        // Randomize the backup number to reduce chance of concurrent backups choosing the same file name
        return ThreadLocalRandom.current().nextInt(1, 100_000_000);
    }
}
