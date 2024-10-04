package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.WorldMap;
import xaero.map.settings.ModOptions;
import xaero.map.settings.ModSettings;
import xaeroplus.settings.XaeroPlusModSettingsHooks;

import java.io.File;
import java.io.IOException;

import static xaeroplus.settings.XaeroPlusSettingsReflectionHax.XAERO_PLUS_WORLDMAP_SETTINGS;

@Mixin(value = ModSettings.class, remap = false)
public class MixinWorldMapModSettings {

    @Shadow
    public int defaultCaveModeType;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void initInject(final CallbackInfo ci) {
        this.defaultCaveModeType = 0; // set default cave mode type to 0
    }

    @Inject(method = "saveSettings", at = @At("TAIL"))
    public void saveSettings(final CallbackInfo ci) throws IOException {
        XaeroPlusModSettingsHooks.saveSettings(WorldMap.optionsFile, XAERO_PLUS_WORLDMAP_SETTINGS);
    }

    @Inject(
        method = "loadSettings", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/settings/ModSettings;saveSettings()V"))
    public void loadSettings(final CallbackInfo ci, @Local(name = "mainConfigFile") File mainConfigFile) throws IOException {
        if (!mainConfigFile.exists()) {
            XaeroPlusModSettingsHooks.loadSettings(null, XAERO_PLUS_WORLDMAP_SETTINGS);
        }
    }

    @Inject(method = "loadSettingsFile", at = @At("TAIL"))
    public void loadSettings(final File file, CallbackInfo ci) throws IOException {
        XaeroPlusModSettingsHooks.loadSettings(file, XAERO_PLUS_WORLDMAP_SETTINGS);
    }

    @Inject(method = "getClientBooleanValue", at = @At("HEAD"), cancellable = true)
    public void getClientBooleanValue(ModOptions o, CallbackInfoReturnable<Boolean> cir) {
        XaeroPlusModSettingsHooks.getClientBooleanValue(o.getEnumString(), XAERO_PLUS_WORLDMAP_SETTINGS, cir);
    }

    @Inject(method = "setOptionValue", at = @At("HEAD"))
    public void setOptionValue(ModOptions o, int par2, final CallbackInfo ci) {
        XaeroPlusModSettingsHooks.setOptionValue(o.getEnumString(), XAERO_PLUS_WORLDMAP_SETTINGS);
    }

    @Inject(method = "setOptionFloatValue", at = @At("HEAD"))
    public void setOptionFloatValue(ModOptions o, double f, CallbackInfo ci) {
        XaeroPlusModSettingsHooks.setOptionFloatValue(o.getEnumString(), f, XAERO_PLUS_WORLDMAP_SETTINGS);
    }

    @Inject(method = "getOptionFloatValue", at = @At("HEAD"), cancellable = true)
    public void getOptionFloatValue(ModOptions o, CallbackInfoReturnable<Double> cir) {
        XaeroPlusModSettingsHooks.getOptionFloatValue(o.getEnumString(), cir, XAERO_PLUS_WORLDMAP_SETTINGS);
    }

    @Inject(method = "getKeyBinding", at = @At("HEAD"), cancellable = true)
    public void getKeyBinding(final ModOptions par1EnumOptions, final CallbackInfoReturnable<String> cir) {
        XaeroPlusModSettingsHooks.getKeybinding(par1EnumOptions.getEnumString(), cir, XAERO_PLUS_WORLDMAP_SETTINGS);
    }
}
