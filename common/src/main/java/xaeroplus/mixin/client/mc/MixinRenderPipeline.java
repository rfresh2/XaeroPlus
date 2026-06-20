package xaeroplus.mixin.client.mc;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.platform.CompareOp;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaeroplus.Globals;

@Mixin(RenderPipeline.class)
public class MixinRenderPipeline {
    @Inject(method = "isCull", at = @At("HEAD"), cancellable = true, remap = false)
    public void overrideIsCull(final CallbackInfoReturnable<Boolean> cir) {
        if (Globals.disableDrawCullingOverride) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getColorTargetState", at = @At("HEAD"), cancellable = true, remap = false)
    public void transparentWmBgOverrideBlendFunction(final CallbackInfoReturnable<ColorTargetState> cir) {
        if (Globals.transparentWmBgApplyMapFrameBlend) {
            cir.setReturnValue(new ColorTargetState(new BlendFunction(BlendFactor.ONE, BlendFactor.ZERO, BlendFactor.ONE, BlendFactor.ZERO)));
        }
        if (Globals.transparentWmBgApplyMapBlend) {
            cir.setReturnValue(new ColorTargetState(new BlendFunction(BlendFactor.ONE, BlendFactor.ZERO, BlendFactor.ONE, BlendFactor.ZERO)));
        }
    }

    @Inject(method = "getColorTargetStates", at = @At("HEAD"), cancellable = true, remap = false)
    public void transparentWmBgOverrideBlendFunctions(final CallbackInfoReturnable<ColorTargetState[]> cir) {
        if (Globals.transparentWmBgApplyMapFrameBlend) {
            cir.setReturnValue(new ColorTargetState[]{new ColorTargetState(new BlendFunction(BlendFactor.ONE, BlendFactor.ZERO, BlendFactor.ONE, BlendFactor.ZERO))});
        }
        if (Globals.transparentWmBgApplyMapBlend) {
            cir.setReturnValue(new ColorTargetState[]{new ColorTargetState(new BlendFunction(BlendFactor.ONE, BlendFactor.ZERO, BlendFactor.ONE, BlendFactor.ZERO))});
        }
    }

    @Inject(method = "getDepthStencilState", at = @At("HEAD"), cancellable = true, remap = false)
    public void transparentWmBgOverrideDepthStencilState(final CallbackInfoReturnable<DepthStencilState> cir) {
        if (Globals.transparentWmBgApplyMapFrameDepthState) {
            cir.setReturnValue(new DepthStencilState(CompareOp.ALWAYS_PASS, false));
        }
    }
}
