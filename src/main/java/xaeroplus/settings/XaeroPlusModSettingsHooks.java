package xaeroplus.settings;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaeroplus.Globals;

import java.io.*;
import java.util.List;
import java.util.Optional;

public class XaeroPlusModSettingsHooks {
    private static int loadCount = 0;

    public static void saveSettings(File configFile, List<XaeroPlusSetting> settings) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(configFile, true))) {
            settings.forEach(k -> {
                writer.println(k.getSettingName() + ":" +
                        ((k instanceof XaeroPlusBooleanSetting)
                                ? ((XaeroPlusBooleanSetting) k).getValue()
                                : (k instanceof XaeroPlusFloatSetting)
                                    ? ((XaeroPlusFloatSetting) k).getValue()
                                    : (k instanceof XaeroPlusEnumSetting)
                                        ? ((XaeroPlusEnumSetting) k).getValueIndex()
                                        : ""));
            });
        }
    }

    public static void loadSettings(File file, List<XaeroPlusSetting> settings) throws IOException {
        loadCount++;
        try(BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String s;
            while ((s = reader.readLine()) != null) {
                String[] args = s.split(":");
                Optional<XaeroPlusSetting> settingOptional = settings.stream()
                        .filter(setting -> setting.getSettingName().equalsIgnoreCase(args[0]))
                        .findFirst();
                if (settingOptional.isPresent()) {
                    final XaeroPlusSetting setting = settingOptional.get();
                    if (setting instanceof XaeroPlusBooleanSetting) {
                        ((XaeroPlusBooleanSetting) setting).setValue(Boolean.parseBoolean(args[1]));
                    } else if (setting instanceof XaeroPlusFloatSetting) {
                        ((XaeroPlusFloatSetting) setting).setValue(Float.parseFloat(args[1]));
                    } else if (setting instanceof XaeroPlusEnumSetting) {
                        ((XaeroPlusEnumSetting) setting).setValueIndex((int) Float.parseFloat(args[1]));
                    }
                }
            }
        }

        // 1 for minimap, 1 for worldmap
        if (loadCount == 2) Globals.onAllSettingsDoneLoading();
    }

    public static void getClientBooleanValue(String enumString, List<XaeroPlusSetting> settings, CallbackInfoReturnable<Boolean> cir) {
        Optional<XaeroPlusBooleanSetting> settingOptional = settings.stream()
                .filter(xaeroPlusSetting -> xaeroPlusSetting.getSettingName().equals(enumString) && xaeroPlusSetting instanceof XaeroPlusBooleanSetting)
                .map(xaeroPlusSetting -> (XaeroPlusBooleanSetting) xaeroPlusSetting)
                .findFirst();
        if (settingOptional.isPresent()) {
            cir.setReturnValue(settingOptional.get().getValue());
            cir.cancel();
        }
    }

    // boolean or enum...
    public static void setOptionValue(String enumString, final Object value, List<XaeroPlusSetting> settings) {
        Optional<XaeroPlusSetting> foundSetting = settings.stream()
                .filter(xaeroPlusSetting -> xaeroPlusSetting.getSettingName().equals(enumString))
                .findFirst();
        foundSetting
                .filter(xaeroPlusSetting -> xaeroPlusSetting instanceof XaeroPlusBooleanSetting && value instanceof Boolean)
                .map(xaeroPlusSetting -> (XaeroPlusBooleanSetting) xaeroPlusSetting)
                .ifPresent(xaeroPlusBooleanSetting -> {
                    xaeroPlusBooleanSetting.setValue((Boolean) value);
                });
        foundSetting
                .filter(xaeroPlusSetting -> xaeroPlusSetting instanceof XaeroPlusEnumSetting && value instanceof Integer && (Integer) value <= ((XaeroPlusEnumSetting<?>) xaeroPlusSetting).getIndexMax() && (Integer) value >= 0)
                .map(xaeroPlusSetting -> (XaeroPlusEnumSetting) xaeroPlusSetting)
                .ifPresent(xaeroPlusEnumSetting -> {
                    xaeroPlusEnumSetting.setValueIndex((Integer) value);
                });
    }

    // boolean or enum...
    public static void getOptionValue(final String enumString, final CallbackInfoReturnable<Object> cir, final List<XaeroPlusSetting> settings) {
        Optional<XaeroPlusSetting> foundSetting = settings.stream()
                .filter(xaeroPlusSetting -> xaeroPlusSetting.getSettingName().equals(enumString))
                .findFirst();
        foundSetting
                .filter(xaeroPlusSetting -> xaeroPlusSetting instanceof XaeroPlusBooleanSetting)
                .map(xaeroPlusSetting -> (XaeroPlusBooleanSetting) xaeroPlusSetting)
                .ifPresent(xaeroPlusBooleanSetting -> {
                    cir.setReturnValue(xaeroPlusBooleanSetting.getValue());
                });
        foundSetting
                .filter(xaeroPlusSetting -> xaeroPlusSetting instanceof XaeroPlusEnumSetting)
                .map(xaeroPlusSetting -> (XaeroPlusEnumSetting) xaeroPlusSetting)
                .ifPresent(xaeroPlusEnumSetting -> {
                    cir.setReturnValue(xaeroPlusEnumSetting.getValueIndex());
                });
    }

    public static void setOptionDoubleValue(String enumString, double f, List<XaeroPlusSetting> settings) {
        Optional<XaeroPlusSetting> foundSetting = settings.stream()
                .filter(xaeroPlusSetting -> xaeroPlusSetting.getSettingName().equals(enumString))
                .findFirst();
        foundSetting
                .filter(xaeroPlusSetting -> xaeroPlusSetting instanceof XaeroPlusFloatSetting)
                .map(xaeroPlusSetting -> (XaeroPlusFloatSetting) xaeroPlusSetting)
                .ifPresent(xaeroPlusSetting -> xaeroPlusSetting.setValue((float) f));
    }

    public static void getOptionDoubleValue(String enumString, CallbackInfoReturnable<Double> cir, List<XaeroPlusSetting> settings) {
        Optional<XaeroPlusSetting> settingOptional = settings.stream()
                .filter(xaeroPlusSetting -> xaeroPlusSetting.getSettingName().equals(enumString))
                .findFirst();
        settingOptional
                .filter(xaeroPlusSetting -> xaeroPlusSetting instanceof XaeroPlusFloatSetting)
                .map(xaeroPlusSetting -> (XaeroPlusFloatSetting) xaeroPlusSetting)
                .ifPresent(xaeroPlusSetting -> {
                    cir.setReturnValue((double) xaeroPlusSetting.getValue());
                    cir.cancel();
                });
    }

    public static void getOptionValueName(final String enumString, final CallbackInfoReturnable<String> cir, final List<XaeroPlusSetting> settings) {
        Optional<XaeroPlusSetting> settingOptional = settings.stream()
                .filter(xaeroPlusSetting -> xaeroPlusSetting.getSettingName().equals(enumString))
                .findFirst();
        settingOptional
                .filter(xaeroPlusSetting -> xaeroPlusSetting instanceof XaeroPlusEnumSetting)
                .map(xaeroPlusSetting -> (XaeroPlusEnumSetting) xaeroPlusSetting)
                .ifPresent(xaeroPlusEnumSetting -> {
                    cir.setReturnValue(xaeroPlusEnumSetting.getValue() instanceof TranslatableSettingEnum
                                               ? ((TranslatableSettingEnum) xaeroPlusEnumSetting.getValue()).getTranslatedName()
                                               : xaeroPlusEnumSetting.getValue().toString());
                });
    }

    public static void getSliderOptionText(final String enumString, final CallbackInfoReturnable<String> cir, final List<XaeroPlusSetting> settings) {
        settings.stream()
                .filter(xaeroPlusSetting -> xaeroPlusSetting.getSettingName().equals(enumString))
                .findFirst()
                .ifPresent(xaeroPlusSetting -> {
                    String s = xaeroPlusSetting.getTranslatedName() + ": ";
                    if (xaeroPlusSetting instanceof XaeroPlusFloatSetting) {
                        s += String.format("%.2f", ((XaeroPlusFloatSetting) xaeroPlusSetting).getValue());
                    } else if (xaeroPlusSetting instanceof XaeroPlusEnumSetting) {
                        XaeroPlusEnumSetting xaeroPlusEnumSetting = (XaeroPlusEnumSetting) xaeroPlusSetting;
                        s += xaeroPlusEnumSetting.getValue() instanceof TranslatableSettingEnum
                                ? ((TranslatableSettingEnum) xaeroPlusEnumSetting.getValue()).getTranslatedName()
                                : xaeroPlusEnumSetting.getValue().toString();
                    }
                    cir.setReturnValue(s);
                });
    }
}
