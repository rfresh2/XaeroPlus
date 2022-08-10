package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.settings.ModSettings;

@Mixin(value = ModSettings.class, remap = false)
public class MixinMinimapModSettings {

    /**
     * experimenting with minimap zoom adjustments
     * need to create further mixins in rendering
     * ideally what we want is as zoom decreases, we get more chunks rendered to fill minimap
     * unfortunately alot of hardcoded values are used in the rendering that need to be changed
     */
    @Shadow
    public float[] zooms;

    @Shadow
    public int caveMaps;

    @Inject(method = "<init>", at = @At(value = "TAIL"))
    private void init(CallbackInfo ci) {
//        zooms = new float[]{0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1.0F, 2.0F, 3.0F, 4.0F, 5.0F};

        // don't show cave maps on minimap by default
        caveMaps = 0;
    }
}
