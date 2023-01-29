package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.WorldMap;
import xaero.map.settings.ModOptions;
import xaero.map.settings.ModSettings;
import xaeroplus.settings.XaeroPlusSetting;
import xaeroplus.settings.XaeroPlusSettingRegistry;

import java.io.*;
import java.util.Optional;

@Mixin(value = ModSettings.class, remap = false)
public class MixinModSettings {

    @Inject(method = "saveSettings", at = @At("TAIL"))
    public void saveSettings(final CallbackInfo ci) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(WorldMap.optionsFile, true))) {
            XaeroPlusSettingRegistry.XAERO_PLUS_SETTING_LIST.forEach(k -> {
                writer.println(k.getSettingName() + ":" + (k.isBooleanSetting() ? k.getBooleanSettingValue() : k.getFloatSettingValue()));
            });
        }
    }

    @Inject(method = "loadSettingsFile", at = @At("TAIL"))
    public void loadSettings(final File file, CallbackInfo ci) throws IOException {
        try(BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String s;
            while ((s = reader.readLine()) != null) {
                String[] args = s.split(":");
                Optional<XaeroPlusSetting> settingOptional = XaeroPlusSettingRegistry.XAERO_PLUS_SETTING_LIST.stream()
                        .filter(setting -> setting.getSettingName().equalsIgnoreCase(args[0]))
                        .findFirst();
                if (settingOptional.isPresent()) {
                    final XaeroPlusSetting setting = settingOptional.get();
                    if (setting.isBooleanSetting()) {
                        setting.setBooleanSettingValue(Boolean.parseBoolean(args[1]));
                    } else {
                        setting.setFloatSettingValue(Float.parseFloat(args[1]));
                    }
                }
            }
        }
    }

    @Inject(method = "getClientBooleanValue", at = @At("HEAD"), cancellable = true)
    public void getClientBooleanValue(ModOptions o, CallbackInfoReturnable<Boolean> cir) {
        Optional<XaeroPlusSetting> settingOptional = XaeroPlusSettingRegistry.XAERO_PLUS_SETTING_LIST.stream()
                .filter(xaeroPlusSetting -> xaeroPlusSetting.getSettingName().equals(o.getEnumString()) && xaeroPlusSetting.isBooleanSetting())
                .findFirst();
        if (settingOptional.isPresent()) {
            cir.setReturnValue(settingOptional.get().getBooleanSettingValue());
            cir.cancel();
        }
    }

    @Inject(method = "setOptionValue", at = @At("HEAD"))
    public void setOptionValue(ModOptions o, int par2, final CallbackInfo ci) {
        Optional<XaeroPlusSetting> settingOptional = XaeroPlusSettingRegistry.XAERO_PLUS_SETTING_LIST.stream()
                .filter(xaeroPlusSetting -> xaeroPlusSetting.getSettingName().equals(o.getEnumString()) && xaeroPlusSetting.isBooleanSetting())
                .findFirst();
        settingOptional.ifPresent(xaeroPlusSetting -> xaeroPlusSetting.setBooleanSettingValue(!xaeroPlusSetting.getBooleanSettingValue()));
    }

    @Inject(method = "setOptionFloatValue", at = @At("HEAD"))
    public void setOptionFloatValue(ModOptions o, double f, CallbackInfo ci) {
        XaeroPlusSettingRegistry.XAERO_PLUS_SETTING_LIST.stream()
                .filter(xaeroPlusSetting -> xaeroPlusSetting.getSettingName().equals(o.getEnumString()) && xaeroPlusSetting.isFloatSetting())
                .findFirst()
                .ifPresent(xaeroPlusSetting -> xaeroPlusSetting.setFloatSettingValue(Double.valueOf(f).floatValue()));
    }

    @Inject(method = "getOptionFloatValue", at = @At("HEAD"), cancellable = true)
    public void getOptionFloatValue(ModOptions o, CallbackInfoReturnable<Double> cir) {
        Optional<XaeroPlusSetting> settingOptional = XaeroPlusSettingRegistry.XAERO_PLUS_SETTING_LIST.stream()
                .filter(xaeroPlusSetting -> xaeroPlusSetting.getSettingName().equals(o.getEnumString()) && xaeroPlusSetting.isFloatSetting())
                .findFirst();
        if (settingOptional.isPresent()) {
            cir.setReturnValue(Float.valueOf(settingOptional.get().getFloatSettingValue()).doubleValue());
            cir.cancel();
        }
    }
}
