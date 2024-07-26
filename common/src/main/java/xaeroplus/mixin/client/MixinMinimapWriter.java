package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import xaero.common.minimap.write.MinimapWriter;
import xaeroplus.Globals;

@Mixin(value = MinimapWriter.class, remap = false)
public class MixinMinimapWriter {

    /**
     * @author
     * @reason
     */
    @Overwrite
    public int getLoadSide() {
        return 9 * Globals.minimapScaleMultiplier;
    }
}
