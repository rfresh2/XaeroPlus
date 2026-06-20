package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import xaero.lib.client.graphics.XaeroRenderType;

@Mixin(value = XaeroRenderType.class, remap = false)
public class MixinXaeroRenderType {

//    @WrapOperation(method = "<clinit>", at = @At(
//        value = "INVOKE",
//        target = "Lcom/mojang/blaze3d/pipeline/RenderPipeline$Builder;build()Lcom/mojang/blaze3d/pipeline/RenderPipeline;",
//        ordinal = 0
//    ),
//        slice = @Slice(
//            from = @At(
//                value = "FIELD",
//                target = "Lxaero/lib/client/graphics/shader/LibShaders;WORLD_MAP:Lnet/minecraft/resources/Identifier;",
//                opcode = Opcodes.GETSTATIC
//            )
//        )
//    )
//    private static RenderPipeline setCustomMapShader(final RenderPipeline.Builder instance, final Operation<RenderPipeline> original) {
//        instance.withBindGroupLayout(BindGroupLayout.builder().withUniform(XaeroPlusShaders.TRANSPARENT_WM_BACKGROUND_UNIFORM.name(), XaeroPlusShaders.TRANSPARENT_WM_BACKGROUND_UNIFORM.type()).build());
//        return original.call(instance);
//    }
}
