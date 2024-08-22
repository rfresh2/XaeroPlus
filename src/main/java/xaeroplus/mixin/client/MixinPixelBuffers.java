package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.graphics.PixelBuffers;

@Mixin(value = PixelBuffers.class, remap = false)
public class MixinPixelBuffers {

    @Shadow private static int buffersType;

    @Inject(method = "<clinit>", at = @At("HEAD"), cancellable = true)
    private static void disableBuffersCheckDuringCITest(final CallbackInfo ci) {
        if (System.getenv("XP_CI_TEST") != null) {
            ci.cancel();
            buffersType = 0;
        }
    }
}
