package xaeroplus.mixin.client.mc;

import com.mojang.blaze3d.platform.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaeroplus.module.impl.FpsLimiter;
import xaeroplus.settings.Settings;

@Mixin(value = GlStateManager.class, remap = false)
public class MixinGlStateManager {
    @Inject(method = "_glBindFramebuffer", at = @At("HEAD"), cancellable = true)
    private static void _glBindFramebuffer(int i, int j, CallbackInfo ci) {
        if (!Settings.REGISTRY.minimapFpsLimiter.get()) return;
        if (FpsLimiter.renderTargetOverwrite != null)
            ci.cancel();
    }
}
