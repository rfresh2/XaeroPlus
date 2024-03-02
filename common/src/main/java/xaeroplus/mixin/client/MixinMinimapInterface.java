package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import xaero.common.minimap.MinimapInterface;

@Mixin(value = MinimapInterface.class, remap = false)
public class MixinMinimapInterface {

    @Shadow
    private Throwable crashedWith;

    /**
     * @author rfresh2
     * @reason Direct support to XaeroPlus
     */
    @Overwrite
    public void checkCrashes() {
        if (this.crashedWith != null) {
            Throwable crash = this.crashedWith;
            this.crashedWith = null;
            throw new RuntimeException("XaeroPlus + Xaero's Minimap has crashed! Please report to: https://github.com/rfresh2/XaeroPlus/issues or https://discord.gg/nJZrSaRKtb", crash);
        }
    }
}
