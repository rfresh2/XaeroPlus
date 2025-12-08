package xaeroplus.mixin.client.mc;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaeroplus.Globals;

import java.util.Optional;

@Mixin(RenderPipeline.class)
public class MixinRenderPipeline {
    @Inject(method = "isCull", at = @At("HEAD"), cancellable = true, remap = false)
    public void overrideIsCull(final CallbackInfoReturnable<Boolean> cir) {
        if (Globals.disableDrawCullingOverride) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getBlendFunction", at = @At("HEAD"), cancellable = true, remap = false)
    public void transparentWmBgOverrideBlendFunction(final CallbackInfoReturnable<Optional<BlendFunction>> cir) {
        if (Globals.transparentWmBgApplyMapFrameBlend) {
            cir.setReturnValue(Optional.of(new BlendFunction(SourceFactor.ONE, DestFactor.ZERO, SourceFactor.ZERO, DestFactor.ONE)));
        }
        if (Globals.transparentWmBgApplyMapBlend) {
            cir.setReturnValue(Optional.of(new BlendFunction(SourceFactor.ONE, DestFactor.ZERO, SourceFactor.ONE, DestFactor.ZERO)));
        }
    }
}
