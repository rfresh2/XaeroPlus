package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import xaero.common.HudMod;
import xaero.common.minimap.write.MinimapWriter;
import xaeroplus.Globals;

@Mixin(value = MinimapWriter.class, remap = false)
public class MixinMinimapWriter {
    @ModifyConstant(method = "getLoadSide", constant = @Constant(intValue = 9))
    public int overrideLoadSide(final int constant) {
        if (HudMod.INSTANCE.getMinimap().usingFBO()) {
            return constant * Globals.minimapScaleMultiplier;
        } else {
            return constant;
        }
    }
}
