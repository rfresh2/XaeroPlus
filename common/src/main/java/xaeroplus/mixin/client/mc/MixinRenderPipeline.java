package xaeroplus.mixin.client.mc;

import com.mojang.blaze3d.pipeline.RenderPipeline;
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
}
