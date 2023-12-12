package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import xaero.map.CrashHandler;

@Mixin(value = CrashHandler.class, remap = false)
public class MixinCrashHandler {
    @Shadow
    private Throwable crashedBy;

    /**
     * @author rfresh2
     * @reason Direct support to XaeroPlus
     */
    @Overwrite
    public void checkForCrashes() throws RuntimeException {
        if (this.crashedBy != null) {
            Throwable crash = this.crashedBy;
            this.crashedBy = null;
            throw new RuntimeException("XaeroPlus + Xaero's World Map has crashed! Please report to: https://github.com/rfresh2/XaeroPlus/issues or https://discord.gg/nJZrSaRKtb", crash);
        }
    }
}
