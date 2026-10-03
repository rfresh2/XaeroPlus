package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xaero.lib.client.graphics.shader.LibShaders;

@Mixin(value = LibShaders.class, remap = false)
public class MixinLibShaders {

    @WrapOperation(method = "<clinit>", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/resources/ResourceLocation;fromNamespaceAndPath(Ljava/lang/String;Ljava/lang/String;)Lnet/minecraft/resources/ResourceLocation;"
    ))
    private static ResourceLocation redirectShaderLocation(final String namespace, final String path, final Operation<ResourceLocation> original) {
        if (path.equals("core/map")) {
            return ResourceLocation.fromNamespaceAndPath("xaeroplus", "custom_map");
        }
        return original.call(namespace, path);
    }
}
