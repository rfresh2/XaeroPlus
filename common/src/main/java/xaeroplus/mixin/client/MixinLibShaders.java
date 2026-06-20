package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import xaero.lib.client.graphics.shader.LibShaders;

@Mixin(value = LibShaders.class, remap = false)
public class MixinLibShaders {
//    @ModifyExpressionValue(method = "<clinit>", at = @At(
//        value = "INVOKE",
//        target = "Lnet/minecraft/resources/Identifier;fromNamespaceAndPath(Ljava/lang/String;Ljava/lang/String;)Lnet/minecraft/resources/Identifier;"
//    ))
//    private static Identifier editShader(final Identifier original) {
//        if (original.getPath().equals("core/map")) {
//            return Identifier.fromNamespaceAndPath("xaeroplus", "custom_map");
//        }
//        return original;
//    }
}
