package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.misc.Misc;
import xaeroplus.Globals;
import xaeroplus.settings.Settings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
        return ThreadLocalRandom.current().nextInt(1, 2_000_000_000);
    }

    @Inject(method = "safeMoveAndReplace", at = @At("HEAD"), cancellable = true)
    private static void atomicMoveAndReplace(final Path from, final Path to, final boolean backupFrom, final CallbackInfo ci) throws IOException {
        if (!Globals.atomicMoveAvailable || !Settings.REGISTRY.atomicMoveAndReplace.get()) return; // fallback to normal xaero logic
        // skip the whole song and dance of multiple moves and deletes, just do an atomic move that replaces the target
        try {
            Files.move(from, to, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            return; // try to fallback to normal xaero logic
        }
        ci.cancel();
    }
}
