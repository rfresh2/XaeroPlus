package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.settings.ModOptions;
import xaero.map.settings.ModSettings;
import xaeroplus.settings.XaeroPlusModSettingsHooks;

import java.io.File;
import java.io.IOException;

import static xaeroplus.settings.XaeroPlusSettingsReflectionHax.ALL_WORLD_MAP_SETTINGS;

@Mixin(value = ModSettings.class, remap = false)
public class MixinWorldMapModSettings {

    @Shadow
    public int defaultCaveModeType;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void initInject(final CallbackInfo ci) {
        this.defaultCaveModeType = 2; // FULL
    }

    @Inject(method = "saveSettings", at = @At(value = "RETURN"))
    public void saveSettings(final CallbackInfo ci) throws IOException {
        XaeroPlusModSettingsHooks.saveSettings();
    }

    @Inject(method = "loadSettingsFile", at = @At("RETURN"))
    public void loadSettings(final File file, CallbackInfo ci) throws IOException {
        XaeroPlusModSettingsHooks.loadSettings(file, ALL_WORLD_MAP_SETTINGS.get());
    }

    @Inject(method = "getClientBooleanValue", at = @At("HEAD"), cancellable = true)
    public void getClientBooleanValue(ModOptions o, CallbackInfoReturnable<Boolean> cir) {
        XaeroPlusModSettingsHooks.getClientBooleanValue(o.getEnumString(), ALL_WORLD_MAP_SETTINGS.get(), cir);
    }

    @Inject(method = "setOptionValue", at = @At("HEAD"))
    public void setOptionValue(ModOptions o, Object value, final CallbackInfo ci) {
        XaeroPlusModSettingsHooks.setOptionValue(o.getEnumString(), value, ALL_WORLD_MAP_SETTINGS.get());
    }

    @Inject(method = "getOptionValue", at = @At("HEAD"), cancellable = true)
    public void getOptionValue(final ModOptions o, final CallbackInfoReturnable<Object> cir) {
        XaeroPlusModSettingsHooks.getOptionValue(o.getEnumString(), cir, ALL_WORLD_MAP_SETTINGS.get());
    }

    @Inject(method = "setOptionDoubleValue", at = @At("HEAD"))
    public void setOptionDoubleValue(ModOptions o, double f, CallbackInfo ci) {
        XaeroPlusModSettingsHooks.setOptionDoubleValue(o.getEnumString(), f, ALL_WORLD_MAP_SETTINGS.get());
    }

    @Inject(method = "getOptionDoubleValue", at = @At("HEAD"), cancellable = true)
    public void getOptionDoubleValue(ModOptions o, CallbackInfoReturnable<Double> cir) {
        XaeroPlusModSettingsHooks.getOptionDoubleValue(o.getEnumString(), cir, ALL_WORLD_MAP_SETTINGS.get());
    }

    @Inject(method = "getOptionValueName", at = @At("HEAD"), cancellable = true)
    public void getOptionValueName(ModOptions o, CallbackInfoReturnable<String> cir) {
        XaeroPlusModSettingsHooks.getOptionValueName(o.getEnumString(), cir, ALL_WORLD_MAP_SETTINGS.get());
    }

    @Inject(method = "getSliderOptionText", at = @At("HEAD"), cancellable = true)
    public void getSliderOptionText(final ModOptions o, final CallbackInfoReturnable<String> cir) {
        XaeroPlusModSettingsHooks.getSliderOptionText(o.getEnumString(), cir, ALL_WORLD_MAP_SETTINGS.get());
    }
}
