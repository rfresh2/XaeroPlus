package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.HudMod;
import xaero.common.minimap.write.MinimapWriter;
import xaeroplus.Globals;

@Mixin(value = MinimapWriter.class, remap = false)
public class MixinMinimapWriter {
    @Inject(method = "getLoadSide", at = @At("RETURN"), cancellable = true)
    public void overrideLoadSideReturn(final CallbackInfoReturnable<Integer> cir) {
        if (HudMod.INSTANCE.getMinimap().usingFBO()) {
            cir.setReturnValue(cir.getReturnValueI() * Globals.minimapScaleMultiplier);
        }
    }
}
