package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;
import xaero.lib.client.graphics.XaeroRenderType;
import xaeroplus.feature.render.shaders.XaeroPlusShaders;

@Mixin(value = XaeroRenderType.class, remap = false)
public class MixinXaeroRenderType {

    @WrapOperation(method = "<clinit>", at = @At(
        value = "INVOKE",
        target = "Lcom/mojang/blaze3d/pipeline/RenderPipeline$Builder;build()Lcom/mojang/blaze3d/pipeline/RenderPipeline;",
        ordinal = 0
    ),
        slice = @Slice(
            from = @At(
                value = "FIELD",
                target = "Lxaero/lib/client/graphics/shader/LibShaders;WORLD_MAP:Lnet/minecraft/resources/Identifier;",
                opcode = Opcodes.GETSTATIC
            )
        )
    )
    private static RenderPipeline setCustomMapShader(final RenderPipeline.Builder instance, final Operation<RenderPipeline> original) {
        instance.withBindGroupLayout(BindGroupLayout.builder().withUniform(XaeroPlusShaders.TRANSPARENT_WM_BACKGROUND_UNIFORM.name(), XaeroPlusShaders.TRANSPARENT_WM_BACKGROUND_UNIFORM.type()).build());
        return original.call(instance);
    }
}
