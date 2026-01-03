package xaeroplus.mixin.client.mc;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.BufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xaeroplus.Globals;

@Mixin(BufferBuilder.class)
public class MixinBufferBuilder {

    @ModifyExpressionValue(method = "beginVertex", at = @At(
        value = "CONSTANT",
        args = "intValue=16777215") // this limit was added in 1.21.10
    )
    public int bypassVertexCountLimit(final int original) {
        // XP can sometimes upload millions of highlight vertices
        // obv not the best for performance, but it's better than crashing
        return Globals.bypassVertexCountLimit
            ? Integer.MAX_VALUE
            : original;
    }
}
