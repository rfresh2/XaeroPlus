package xaeroplus.mixin.client.mc;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.BufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BufferBuilder.class)
public class MixinBufferBuilder {

    @ModifyExpressionValue(method = "beginVertex", at = @At(
        value = "CONSTANT",
        args = "intValue=16777215")
    )
    public int removeVertexCountLimit(final int original) {
        return Integer.MAX_VALUE;
    }
}
