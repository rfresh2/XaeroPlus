package xaeroplus.fabric.gametest.mixin;

import com.mojang.text2speech.Narrator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Narrator.class)
public interface MixinNarrator {
    @Inject(method = "getNarrator", at = @At("HEAD"), cancellable = true)
    private static void getNarrator(final CallbackInfoReturnable<Narrator> cir) {
        cir.setReturnValue(Narrator.EMPTY);
    }
}
