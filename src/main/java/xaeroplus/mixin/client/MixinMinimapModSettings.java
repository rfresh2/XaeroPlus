package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.AXaeroMinimap;
import xaero.common.settings.ModOptions;
import xaero.common.settings.ModSettings;
import xaeroplus.settings.XaeroPlusModSettingsHooks;

import java.io.File;
import java.io.IOException;

import static xaeroplus.settings.XaeroPlusSettingsReflectionHax.ALL_MINIMAP_SETTINGS;
import static xaeroplus.settings.XaeroPlusSettingsReflectionHax.XAERO_PLUS_WORLDMAP_SETTINGS;

@Mixin(value = ModSettings.class, remap = false)
public class MixinMinimapModSettings {

    @Shadow
    public int caveMaps;

    @Shadow
    protected AXaeroMinimap modMain;

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void init(CallbackInfo ci) {
        // don't show cave maps on minimap by default
        caveMaps = 0;
    }


    @Inject(method = "saveSettings", at = @At("TAIL"))
    public void saveSettings(final CallbackInfo ci) throws IOException {
        XaeroPlusModSettingsHooks.saveSettings(this.modMain.getConfigFile(), ALL_MINIMAP_SETTINGS.get());
    }

    @Inject(method = "loadSettingsFile", at = @At("TAIL"))
    public void loadSettings(final File file, CallbackInfo ci) throws IOException {
        XaeroPlusModSettingsHooks.loadSettings(file, ALL_MINIMAP_SETTINGS.get());
    }

    @Inject(method = "getClientBooleanValue", at = @At("HEAD"), cancellable = true)
    public void getClientBooleanValue(ModOptions o, CallbackInfoReturnable<Boolean> cir) {
        XaeroPlusModSettingsHooks.getClientBooleanValue(o.getEnumString(), ALL_MINIMAP_SETTINGS.get(), cir);
    }

    @Inject(method = "setOptionValue", at = @At("HEAD"))
    public void setOptionValue(ModOptions o, Object value, final CallbackInfo ci) {
        XaeroPlusModSettingsHooks.setOptionValue(o.getEnumString(), value, ALL_MINIMAP_SETTINGS.get());
    }

    @Inject(method = "getOptionValue", at = @At("HEAD"), cancellable = true)
    public void getOptionValue(final ModOptions o, final CallbackInfoReturnable<Object> cir) {
        XaeroPlusModSettingsHooks.getOptionValue(o.getEnumString(), cir, ALL_MINIMAP_SETTINGS.get());
    }

    @Inject(method = "setOptionDoubleValue", at = @At("HEAD"))
    public void setOptionFloatValue(ModOptions o, double f, CallbackInfo ci) {
        XaeroPlusModSettingsHooks.setOptionDoubleValue(o.getEnumString(), f, ALL_MINIMAP_SETTINGS.get());
    }

    @Inject(method = "getOptionDoubleValue", at = @At("HEAD"), cancellable = true)
    public void getOptionFloatValue(ModOptions o, CallbackInfoReturnable<Double> cir) {
        XaeroPlusModSettingsHooks.getOptionDoubleValue(o.getEnumString(), cir, ALL_MINIMAP_SETTINGS.get());
    }

    @Inject(method = "getOptionValueName", at = @At("HEAD"), cancellable = true)
    public void getOptionValueName(ModOptions o, CallbackInfoReturnable<String> cir) {
        XaeroPlusModSettingsHooks.getOptionValueName(o.getEnumString(), cir, ALL_MINIMAP_SETTINGS.get());
    }
}

