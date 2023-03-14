package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.IXaeroMinimap;
import xaero.common.settings.ModOptions;
import xaero.common.settings.ModSettings;
import xaeroplus.settings.XaeroPlusModSettingsHooks;

import java.io.File;
import java.io.IOException;

import static xaeroplus.settings.XaeroPlusSettingsReflectionHax.XAERO_PLUS_MINIMAP_OVERLAY_SETTINGS;

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

    @Shadow
    private IXaeroMinimap modMain;

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void init(CallbackInfo ci) {
//        zooms = new float[]{0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1.0F, 2.0F, 3.0F, 4.0F, 5.0F};

        // don't show cave maps on minimap by default
        caveMaps = 0;
    }


    @Inject(method = "saveSettings", at = @At("TAIL"))
    public void saveSettings(final CallbackInfo ci) throws IOException {
        XaeroPlusModSettingsHooks.saveSettings(this.modMain.getConfigFile(), XAERO_PLUS_MINIMAP_OVERLAY_SETTINGS);
    }

    @Inject(method = "loadSettingsFile", at = @At("TAIL"))
    public void loadSettings(final File file, CallbackInfo ci) throws IOException {
        XaeroPlusModSettingsHooks.loadSettings(file, XAERO_PLUS_MINIMAP_OVERLAY_SETTINGS);
    }

    @Inject(method = "getClientBooleanValue", at = @At("HEAD"), cancellable = true)
    public void getClientBooleanValue(ModOptions o, CallbackInfoReturnable<Boolean> cir) {
        XaeroPlusModSettingsHooks.getClientBooleanValue(o.getEnumString(), XAERO_PLUS_MINIMAP_OVERLAY_SETTINGS, cir);
    }

    @Inject(method = "setOptionValue", at = @At("HEAD"))
    public void setOptionValue(ModOptions o, int par2, final CallbackInfo ci) {
        XaeroPlusModSettingsHooks.setOptionValue(o.getEnumString(), XAERO_PLUS_MINIMAP_OVERLAY_SETTINGS);
    }

    @Inject(method = "setOptionFloatValue", at = @At("HEAD"))
    public void setOptionFloatValue(ModOptions o, double f, CallbackInfo ci) {
        XaeroPlusModSettingsHooks.setOptionFloatValue(o.getEnumString(), f, XAERO_PLUS_MINIMAP_OVERLAY_SETTINGS);
    }

    @Inject(method = "getOptionFloatValue", at = @At("HEAD"), cancellable = true)
    public void getOptionFloatValue(ModOptions o, CallbackInfoReturnable<Double> cir) {
        XaeroPlusModSettingsHooks.getOptionFloatValue(o.getEnumString(), cir, XAERO_PLUS_MINIMAP_OVERLAY_SETTINGS);
    }

    @Inject(method = "getKeyBinding", at = @At("HEAD"), cancellable = true)
    public void getKeybinding(final ModOptions par1EnumOptions, final CallbackInfoReturnable<String> cir) {
        XaeroPlusModSettingsHooks.getKeybinding(par1EnumOptions.getEnumString(), cir, XAERO_PLUS_MINIMAP_OVERLAY_SETTINGS);
    }
}
