package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.region.Overlay;
import xaeroplus.settings.Settings;

@Mixin(value = Overlay.class, remap = false)
public class MixinOverlay {
    @Shadow private byte opacity;

    @Inject(method = "increaseOpacity", at = @At("HEAD"), cancellable = true)
    public void increaseOpacity(final int toAdd, final CallbackInfo ci) {
        if (Settings.REGISTRY.overlayOpacityFix.get()) {
            ci.cancel();

            // fix byte overflow
            // without this patch, opacity can be increased into negative numbers, increasing brightness instead of decreasing

            // found as this was causing 128+ block high water columns to appear abnormally bright
            int newOpacity = this.opacity + toAdd;
            if (newOpacity > 15) newOpacity = 15;
            // todo: no negative check here, is there any valid use case with negative opacity?
            this.opacity = (byte) (newOpacity);
        }
    }
}
