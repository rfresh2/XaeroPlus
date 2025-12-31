package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.lib.client.graphics.shader.WorldMapShaderHelper;
import xaeroplus.feature.render.shaders.XaeroPlusShaders;

@Mixin(value = WorldMapShaderHelper.class, remap = false)
public class MixinWorldMapShaderHelper {

    @Inject(method = "ensureUniforms", at = @At("RETURN"))
    private static void ensureCustomUniforms(final CallbackInfo ci) {
        XaeroPlusShaders.ensureTransparentBackgroundUniforms();
    }
}
