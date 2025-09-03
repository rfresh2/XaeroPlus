package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xaero.hud.minimap.world.io.MinimapWorldManagerIO;

import java.nio.file.Path;
import java.util.concurrent.ThreadLocalRandom;

@Mixin(value = MinimapWorldManagerIO.class, remap = false)
public class MixinMinimapWorldManagerIO {
    @WrapOperation(method = "saveWorld(Lxaero/hud/minimap/world/MinimapWorld;Z)V", at = @At(
        value = "INVOKE",
        target = "Ljava/nio/file/Path;resolve(Ljava/lang/String;)Ljava/nio/file/Path;",
        ordinal = 0
    ))
    public Path reduceChanceOfFileNameCollisions(final Path instance, final String other, final Operation<Path> original) {
        // reduce chance of file name collisions from concurrent mc instances
        return original.call(instance, other + "-xp-" + ThreadLocalRandom.current().nextInt(0, 2_000_000_000));
    }
}
