package xaeroplus.mixin.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.graphics.ImprovedFramebuffer;
import xaeroplus.settings.Settings;

@Mixin(value = ImprovedFramebuffer.class, remap = false)
public class MixinImprovedFramebuffer {

    @Shadow
    private static RenderTarget mainRenderTargetBackup;

    @Inject(method = "restoreMainRenderTarget", at = @At("RETURN"))
    private static void clearCachedMainRenderTarget(final CallbackInfo ci) {
        if (Settings.REGISTRY.minimapFpsLimiter.get()) {
            mainRenderTargetBackup = null;
        }
    }
}
