package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL30;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xaero.common.graphics.ImprovedFramebuffer;
import xaeroplus.settings.Settings;

@Mixin(value = ImprovedFramebuffer.class)
public class MixinImprovedFramebuffer {

    @WrapOperation(method = "resize", at = @At(
        value = "INVOKE",
        target = "Lxaero/common/graphics/ImprovedFramebuffer;detectOptifineFBOs()V"
    ))
    public void wrapDetectOptifineFBOs(final Operation<Void> original) {
        if (!Settings.REGISTRY.minimapFpsLimiter.get()) original.call();
    }

    @WrapOperation(method = "beginWrite", at = @At(
        value = "INVOKE",
        target = "Lcom/mojang/blaze3d/platform/GlStateManager;_glBindFramebuffer(II)V"
    ), remap = false)
    private static void redirectBindFramebuffer(final int i, final int j, final Operation<Void> original) {
        if (!Settings.REGISTRY.minimapFpsLimiter.get()) original.call(i, j);
        // sidestep our redirect mixin in GlStateManager to allow xaero to bind and render to its custom FBOs
        RenderSystem.assertOnRenderThreadOrInit();
        GL30.glBindFramebuffer(i, j);
    }
}
