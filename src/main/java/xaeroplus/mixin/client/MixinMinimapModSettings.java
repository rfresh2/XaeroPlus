package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.settings.ModSettings;

@Mixin(value = ModSettings.class, remap = false)
public class MixinMinimapModSettings {

    @Shadow
    public float[] zooms;

    @Inject(method = "<init>", at = @At(value = "TAIL"))
    private void init(CallbackInfo ci) {
        zooms = new float[]{0.75f, 1.0F, 2.0F, 3.0F, 4.0F, 5.0F};
    }
}
