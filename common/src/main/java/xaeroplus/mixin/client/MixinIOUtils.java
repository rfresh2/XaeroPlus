package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.lib.common.util.IOUtils;
import xaeroplus.Globals;
import xaeroplus.XaeroPlus;
import xaeroplus.settings.Settings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ThreadLocalRandom;

@Mixin(value = IOUtils.class, remap = false)
public class MixinIOUtils {
    @WrapOperation(method = "quickFileBackupMove", at = @At(
        value = "INVOKE",
        target = "Ljava/nio/file/Path;resolveSibling(Ljava/lang/String;)Ljava/nio/file/Path;",
        ordinal = 0
    ))
    private static Path randomizeBackupPath(final Path instance, final String pathArg, final Operation<Path> original) {
        // reduce chance of file name collisions from concurrent mc instances
        return original.call(instance, pathArg + "-xp-" + ThreadLocalRandom.current().nextInt(0, 2_000_000_000));
    }

    @Inject(method = "safeMoveAndReplace", at = @At("HEAD"), cancellable = true)
    private static void atomicMoveAndReplace(final Path from, final Path to, final boolean backupFrom, final CallbackInfo ci) throws IOException {
        if (!Globals.atomicMoveAvailable || !Settings.REGISTRY.atomicMoveAndReplace.get()) return; // fallback to normal xaero logic
        // skip the whole song and dance of multiple moves and deletes, just do an atomic move that replaces the target
        try {
            Files.move(from, to, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            ci.cancel();
        } catch (Exception e) {
            XaeroPlus.LOGGER.debug("Failed atomic move, falling back to normal xaero logic", e);
            Settings.REGISTRY.atomicMoveAndReplace.setValue(false);
            // fall through to normal xaero logic
        }
    }
}
