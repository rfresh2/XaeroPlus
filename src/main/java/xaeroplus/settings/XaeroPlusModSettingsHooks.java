package xaeroplus.settings;

import net.minecraft.client.resources.I18n;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaeroplus.util.Shared;

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
        if (loadCount == 2) Shared.onAllSettingsLoaded();
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

    public static void setOptionValue(String enumString, List<XaeroPlusSetting> settings) {
        Optional<XaeroPlusBooleanSetting> settingOptional = settings.stream()
                .filter(xaeroPlusSetting -> xaeroPlusSetting.getSettingName().equals(enumString) && xaeroPlusSetting instanceof XaeroPlusBooleanSetting)
                .map(xaeroPlusSetting -> (XaeroPlusBooleanSetting) xaeroPlusSetting)
                .findFirst();
        settingOptional.ifPresent(xaeroPlusSetting -> xaeroPlusSetting.setValue(!xaeroPlusSetting.getValue()));
    }

    public static void setOptionFloatValue(String enumString, double f, List<XaeroPlusSetting> settings) {
        Optional<XaeroPlusSetting> foundSetting = settings.stream()
                .filter(xaeroPlusSetting -> xaeroPlusSetting.getSettingName().equals(enumString))
                .findFirst();
        foundSetting
                .filter(xaeroPlusSetting ->  xaeroPlusSetting instanceof XaeroPlusFloatSetting)
                .map(xaeroPlusSetting -> (XaeroPlusFloatSetting) xaeroPlusSetting)
                .ifPresent(xaeroPlusSetting -> xaeroPlusSetting.setValue((float) f));
        // why does xaero not have proper enum settings??? They are "floats" but treated like int indeces. just...why...
        foundSetting
                .filter(xaeroPlusSetting ->  xaeroPlusSetting instanceof XaeroPlusEnumSetting)
                .map(xaeroPlusSetting -> (XaeroPlusEnumSetting) xaeroPlusSetting)
                .ifPresent(xaeroPlusEnumSetting -> xaeroPlusEnumSetting.setValueIndex((int) f));
    }

    public static void getOptionFloatValue(String enumString, CallbackInfoReturnable<Double> cir, List<XaeroPlusSetting> settings) {
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
        // i'm debating whether to continue this enum madness or just mixin config save/load to write enums as strings...
        settingOptional
                .filter(xaeroPlusSetting -> xaeroPlusSetting instanceof XaeroPlusEnumSetting)
                .map(xaeroPlusSetting -> (XaeroPlusEnumSetting) xaeroPlusSetting)
                .ifPresent(xaeroPlusEnumSetting -> {
                    cir.setReturnValue((double) xaeroPlusEnumSetting.getValueIndex());
                    cir.cancel();
                });
    }

    public static void getKeybinding(final String enumString, final CallbackInfoReturnable<String> cir, final List<XaeroPlusSetting> settings) {
        Optional<XaeroPlusSetting> foundSetting = settings.stream()
                .filter(xaeroPlusSetting -> xaeroPlusSetting.getSettingName().equals(enumString))
                .findFirst();
        foundSetting
                .filter(xaeroPlusSetting -> xaeroPlusSetting instanceof XaeroPlusBooleanSetting)
                .map(xaeroPlusSetting -> (XaeroPlusBooleanSetting) xaeroPlusSetting)
                .ifPresent(xaeroPlusBooleanSetting -> cir.setReturnValue(
                        xaeroPlusBooleanSetting.getTranslatedName() + ": "
                        + I18n.format("gui.xaero_" + (xaeroPlusBooleanSetting.getValue() ? "on" : "off"))));
        foundSetting
                .filter(xaeroPlusSetting -> xaeroPlusSetting instanceof XaeroPlusEnumSetting)
                .map(xaeroPlusSetting -> (XaeroPlusEnumSetting) xaeroPlusSetting)
                .ifPresent(xaeroPlusEnumSetting -> cir.setReturnValue(xaeroPlusEnumSetting.getTranslatedName() + ": "
                            + (xaeroPlusEnumSetting.getValue() instanceof TranslatableSettingEnum
                            ? ((TranslatableSettingEnum) xaeroPlusEnumSetting.getValue()).getTranslatedName()
                            : xaeroPlusEnumSetting.getValue().toString())));
        foundSetting
                .filter(xaeroPlusSetting -> xaeroPlusSetting instanceof XaeroPlusFloatSetting)
                .map(xaeroPlusSetting -> (XaeroPlusFloatSetting) xaeroPlusSetting)
                .ifPresent(xaeroPlusFloatSetting -> {
                    final int intCastStep = (int) xaeroPlusFloatSetting.getValueStep();
                    if (xaeroPlusFloatSetting.getValueStep() - intCastStep <= 0) {
                        // this float is equivalent to an int
                        cir.setReturnValue(xaeroPlusFloatSetting.getTranslatedName() + ": " + ((int) xaeroPlusFloatSetting.getValue()));
                    } else {
                        cir.setReturnValue(xaeroPlusFloatSetting.getTranslatedName() + ": " + xaeroPlusFloatSetting.getValue());
                    }
                });
    }
}
