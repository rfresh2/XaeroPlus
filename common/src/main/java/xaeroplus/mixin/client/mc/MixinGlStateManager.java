package xaeroplus.mixin.client.mc;

import com.mojang.blaze3d.platform.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = GlStateManager.class, remap = false)
public class MixinGlStateManager {
    // TODO: FIX ON FORGE

//    @Inject(method = "_glBindFramebuffer(II)V", at = @At("HEAD"), cancellable = true)
//    private static void _glBindFramebuffer(int i, int j, CallbackInfo ci) {
//        if (!XaeroPlusSettingRegistry.minimapFpsLimiter.getValue()) return;
//        if (FpsLimiter.renderTargetOverwrite != null)
//            ci.cancel();
//    }
}
