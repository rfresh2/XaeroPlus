package xaeroplus.mixin.client;

import net.minecraft.client.renderer.ShaderProgram;
import net.minecraft.resources.ResourceLocation;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.lib.client.graphics.shader.LibShaders;
import xaero.map.graphics.CustomRenderTypes;

@Mixin(value = CustomRenderTypes.class, remap = false)
public class MixinWMCustomRenderTypes {
    @Inject(method = "<clinit>", at = @At(
        value = "FIELD",
        target = "Lxaero/lib/client/graphics/shader/LibShaders;WORLD_MAP:Lnet/minecraft/client/renderer/ShaderProgram;",
        opcode = Opcodes.GETSTATIC,
        ordinal = 0
    ))
    private static void setCustomMapShader(final CallbackInfo ci) {
        LibShaders.WORLD_MAP = new ShaderProgram(ResourceLocation.fromNamespaceAndPath("xaeroplus", "custom_map"), LibShaders.WORLD_MAP.vertexFormat(), LibShaders.WORLD_MAP.defines());
    }
}
