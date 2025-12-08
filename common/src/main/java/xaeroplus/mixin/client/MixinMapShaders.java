package xaeroplus.mixin.client;

import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xaero.map.graphics.shader.MapShaders;

@Mixin(value = MapShaders.class, remap = false)
public class MixinMapShaders {
    @Redirect(method = "<clinit>", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/resources/ResourceLocation;fromNamespaceAndPath(Ljava/lang/String;Ljava/lang/String;)Lnet/minecraft/resources/ResourceLocation;",
        ordinal = 0
    ))
    private static ResourceLocation editShader(final String namespace, final String path) {
        return ResourceLocation.fromNamespaceAndPath("xaeroplus", "custom_map");
    }
}
