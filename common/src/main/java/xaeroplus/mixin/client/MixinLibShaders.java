package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xaero.lib.client.graphics.shader.LibShaders;

@Mixin(value = LibShaders.class, remap = false)
public class MixinLibShaders {

    @ModifyExpressionValue(method = "onResourceReload", at = @At(
        value = "CONSTANT",
        args = "stringValue=xaerolib/map"
    ))
    private static String customWorldMapShader(final String original) {
        return "xaeroplus/custom_map";
    }
}
