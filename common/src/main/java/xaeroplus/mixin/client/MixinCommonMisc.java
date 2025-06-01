package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xaero.common.misc.Misc;

import java.util.concurrent.ThreadLocalRandom;

@Mixin(value = Misc.class, remap = false)
public class MixinCommonMisc {
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
