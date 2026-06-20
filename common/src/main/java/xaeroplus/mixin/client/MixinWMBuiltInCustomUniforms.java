package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import xaero.lib.client.graphics.shader.BuiltInCustomUniforms;

@Mixin(value = BuiltInCustomUniforms.class, remap = false)
public class MixinWMBuiltInCustomUniforms {

//    @Inject(method = "registerAll", at = @At("RETURN"))
//    private static void registerCustomMapUniform(final CallbackInfo ci) {
//        CustomUniforms.register(XaeroPlusShaders.TRANSPARENT_WM_BACKGROUND_UNIFORM);
//        XaeroPlusShaders.setTransparentWMBackground(false);
//    }
}
