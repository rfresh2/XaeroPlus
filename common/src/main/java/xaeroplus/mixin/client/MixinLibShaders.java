package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xaero.lib.client.graphics.shader.LibShaders;

@Mixin(value = LibShaders.class, remap = false)
public class MixinLibShaders {
    @ModifyExpressionValue(method = "<clinit>", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/resources/ResourceLocation;fromNamespaceAndPath(Ljava/lang/String;Ljava/lang/String;)Lnet/minecraft/resources/ResourceLocation;"
    ))
    private static ResourceLocation editShader(final ResourceLocation original) {
        if (original.getPath().equals("core/map")) {
            return ResourceLocation.fromNamespaceAndPath("xaeroplus", "custom_map");
        }
        return original;
    }
}
