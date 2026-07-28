package xaeroplus.fabric.gametest.mixin;

import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
public class MixinSoundEngine {
    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    public void play(CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
    }
}
