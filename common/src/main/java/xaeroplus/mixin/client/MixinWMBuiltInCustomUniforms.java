package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.lib.client.graphics.shader.BuiltInCustomUniforms;
import xaero.lib.client.graphics.shader.CustomUniforms;
import xaeroplus.feature.render.shaders.XaeroPlusShaders;

@Mixin(value = BuiltInCustomUniforms.class, remap = false)
public class MixinWMBuiltInCustomUniforms {

    @Inject(method = "registerAll", at = @At("RETURN"))
    private static void registerCustomMapUniform(final CallbackInfo ci) {
        CustomUniforms.register(XaeroPlusShaders.TRANSPARENT_WM_BACKGROUND_UNIFORM);
        XaeroPlusShaders.setTransparentWMBackground(false);
    }
}
