package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import xaero.common.minimap.MinimapInterface;
import xaero.common.minimap.write.MinimapWriter;
import xaeroplus.Globals;

@Mixin(value = MinimapWriter.class, remap = false)
public class MixinMinimapWriter {

    @Shadow private MinimapInterface minimapInterface;

    @ModifyConstant(method = "getLoadSide", constant = @Constant(intValue = 9))
    public int overrideLoadSide(final int constant) {
        if (this.minimapInterface.usingFBO()) {
            return constant * Globals.minimapScaleMultiplier;
        } else {
            return constant;
        }
    }
}
