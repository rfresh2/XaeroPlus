package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.lib.client.graphics.XaeroBufferProvider;
import xaero.map.graphics.CustomRenderTypes;
import xaeroplus.feature.render.shaders.XaeroPlusShaders;

@Mixin(value = CustomRenderTypes.class, remap = false)
public class MixinCustomRenderTypes {
    @Inject(method = "applyFixedOrder", at = @At(
        value = "INVOKE",
        target = "Lxaero/lib/client/graphics/XaeroBufferProvider;addToFixedOrder(Lnet/minecraft/client/renderer/rendertype/RenderType;)V",
        ordinal = 0
    ),
    slice = @Slice(
        from = @At(
            value = "FIELD",
            opcode = Opcodes.GETSTATIC,
            target = "Lxaero/map/graphics/CustomRenderTypes;MAP:Lnet/minecraft/client/renderer/rendertype/RenderType;"
        )
    ))
    private static void injectCustomRenderTypes(CallbackInfo ci, @Local(name = "bufferProvider") XaeroBufferProvider bufferProvider) {
        bufferProvider.addToFixedOrder(XaeroPlusShaders.CUSTOM_MAP_FRAME);
        bufferProvider.addToFixedOrder(XaeroPlusShaders.CUSTOM_MAP);
    }
}
