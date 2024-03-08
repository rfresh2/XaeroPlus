package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xaero.common.graphics.ImprovedFramebuffer;
import xaeroplus.settings.XaeroPlusSettingRegistry;

@Mixin(value = ImprovedFramebuffer.class)
public class MixinImprovedFramebuffer {

    @WrapOperation(method = "resize", at = @At(
        value = "INVOKE",
        target = "Lxaero/common/graphics/ImprovedFramebuffer;detectOptifineFBOs()V"
    ))
    public void wrapDetectOptifineFBOs(final Operation<Void> original) {
        if (!XaeroPlusSettingRegistry.minimapFpsLimiter.getValue()) original.call();
    }

    // TODO: FIX ON FORGE
//    @WrapOperation(method = "beginWrite", at = @At(
//        value = "INVOKE",
//        target = "Lcom/mojang/blaze3d/platform/GlStateManager;_glBindFramebuffer(II)V"
//    ), remap = false)
//    private static void redirectBindFramebuffer(final int i, final int j, final Operation<Void> original) {
//        if (!XaeroPlusSettingRegistry.minimapFpsLimiter.getValue()) original.call(i, j);
//        // sidestep our redirect mixin in GlStateManager to allow xaero to bind and render to its custom FBOs
//        RenderSystem.assertOnRenderThreadOrInit();
//        GL30.glBindFramebuffer(i, j);
//    }
}
