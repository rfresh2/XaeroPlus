package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xaero.common.HudMod;
import xaero.common.minimap.write.MinimapWriter;
import xaeroplus.Globals;

@Mixin(value = MinimapWriter.class, remap = false)
public class MixinMinimapWriter {
    @ModifyExpressionValue(
        method = "getLoadSide",
        at = @At(
            value = "CONSTANT",
            args = "intValue=9"
        )
    )
    public int overrideLoadSide(final int constant) {
        if (HudMod.INSTANCE.getMinimap().usingFBO()) {
            return constant * Globals.minimapScaleMultiplier;
        } else {
            return constant;
        }
    }
}
