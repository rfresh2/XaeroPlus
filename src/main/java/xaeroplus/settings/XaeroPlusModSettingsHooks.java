package xaeroplus.settings;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.*;
import java.util.List;
import java.util.Optional;

public class XaeroPlusModSettingsHooks {

    public static void saveSettings(File configFile, List<XaeroPlusSetting> settings) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(configFile, true))) {
            settings.forEach(k -> {
                writer.println(k.getSettingName() + ":" + (k.isBooleanSetting() ? k.getBooleanSettingValue() : k.getFloatSettingValue()));
            });
        }
    }

    public static void loadSettings(File file, List<XaeroPlusSetting> settings) throws IOException {
        try(BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String s;
            while ((s = reader.readLine()) != null) {
                String[] args = s.split(":");
                Optional<XaeroPlusSetting> settingOptional = settings.stream()
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

    public static void getClientBooleanValue(String enumString, List<XaeroPlusSetting> settings, CallbackInfoReturnable<Boolean> cir) {
        Optional<XaeroPlusSetting> settingOptional = settings.stream()
                .filter(xaeroPlusSetting -> xaeroPlusSetting.getSettingName().equals(enumString) && xaeroPlusSetting.isBooleanSetting())
                .findFirst();
        if (settingOptional.isPresent()) {
            cir.setReturnValue(settingOptional.get().getBooleanSettingValue());
            cir.cancel();
        }
    }

    public static void setOptionValue(String enumString, List<XaeroPlusSetting> settings) {
        Optional<XaeroPlusSetting> settingOptional = settings.stream()
                .filter(xaeroPlusSetting -> xaeroPlusSetting.getSettingName().equals(enumString) && xaeroPlusSetting.isBooleanSetting())
                .findFirst();
        settingOptional.ifPresent(xaeroPlusSetting -> xaeroPlusSetting.setBooleanSettingValue(!xaeroPlusSetting.getBooleanSettingValue()));
    }

    public static void setOptionFloatValue(String enumString, double f, List<XaeroPlusSetting> settings) {
        settings.stream()
                .filter(xaeroPlusSetting -> xaeroPlusSetting.getSettingName().equals(enumString) && xaeroPlusSetting.isFloatSetting())
                .findFirst()
                .ifPresent(xaeroPlusSetting -> xaeroPlusSetting.setFloatSettingValue((float) f));
    }

    public static void getOptionFloatValue(String enumString, CallbackInfoReturnable<Double> cir, List<XaeroPlusSetting> settings) {
        Optional<XaeroPlusSetting> settingOptional = settings.stream()
                .filter(xaeroPlusSetting -> xaeroPlusSetting.getSettingName().equals(enumString) && xaeroPlusSetting.isFloatSetting())
                .findFirst();
        if (settingOptional.isPresent()) {
            cir.setReturnValue((double)settingOptional.get().getFloatSettingValue());
            cir.cancel();
        }
    }
}
