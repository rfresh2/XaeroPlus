package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.WorldMap;
import xaero.map.settings.ModOptions;
import xaero.map.settings.ModSettings;
import xaeroplus.settings.XaeroPlusModSettingsHooks;
import xaeroplus.settings.XaeroPlusSettingRegistry;

import java.io.*;

@Mixin(value = ModSettings.class, remap = false)
public class MixinWorldMapModSettings {

    @Inject(method = "saveSettings", at = @At("TAIL"))
    public void saveSettings(final CallbackInfo ci) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(WorldMap.optionsFile, true))) {
            XaeroPlusSettingRegistry.XAERO_PLUS_WORLDMAP_SETTINGS.forEach(k -> {
                writer.println(k.getSettingName() + ":" + (k.isBooleanSetting() ? k.getBooleanSettingValue() : k.getFloatSettingValue()));
            });
        }
    }

    @Inject(method = "loadSettingsFile", at = @At("TAIL"))
    public void loadSettings(final File file, CallbackInfo ci) throws IOException {
        XaeroPlusModSettingsHooks.loadSettings(file, XaeroPlusSettingRegistry.XAERO_PLUS_WORLDMAP_SETTINGS);
    }

    @Inject(method = "getClientBooleanValue", at = @At("HEAD"), cancellable = true)
    public void getClientBooleanValue(ModOptions o, CallbackInfoReturnable<Boolean> cir) {
        XaeroPlusModSettingsHooks.getClientBooleanValue(o.getEnumString(), XaeroPlusSettingRegistry.XAERO_PLUS_WORLDMAP_SETTINGS, cir);
    }

    @Inject(method = "setOptionValue", at = @At("HEAD"))
    public void setOptionValue(ModOptions o, int par2, final CallbackInfo ci) {
        XaeroPlusModSettingsHooks.setOptionValue(o.getEnumString(), XaeroPlusSettingRegistry.XAERO_PLUS_WORLDMAP_SETTINGS);
    }

    @Inject(method = "setOptionFloatValue", at = @At("HEAD"))
    public void setOptionFloatValue(ModOptions o, double f, CallbackInfo ci) {
        XaeroPlusModSettingsHooks.setOptionFloatValue(o.getEnumString(), f, XaeroPlusSettingRegistry.XAERO_PLUS_WORLDMAP_SETTINGS);
    }

    @Inject(method = "getOptionFloatValue", at = @At("HEAD"), cancellable = true)
    public void getOptionFloatValue(ModOptions o, CallbackInfoReturnable<Double> cir) {
        XaeroPlusModSettingsHooks.getOptionFloatValue(o.getEnumString(), cir, XaeroPlusSettingRegistry.XAERO_PLUS_WORLDMAP_SETTINGS);
    }
}
