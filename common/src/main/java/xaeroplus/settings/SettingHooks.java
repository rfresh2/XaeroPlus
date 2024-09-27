package xaeroplus.settings;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaeroplus.XaeroPlus;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class SettingHooks {
    public static void saveSettings() {
        try {
            saveXPSettings();
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Failed saving settings", e);
        }
    }

    public static synchronized void saveXPSettings() throws IOException {
        File tempFile = new File(XaeroPlus.configFile.getAbsolutePath() + ".tmp");
        if (tempFile.exists()) tempFile.delete();
        try (PrintWriter writer = new PrintWriter(new FileWriter(tempFile, true))) {
            var allSettings = Settings.REGISTRY.getAllSettings();
            for (int i = 0; i < allSettings.size(); i++) {
                final XaeroPlusSetting setting = allSettings.get(i);
                writer.println(setting.getSettingName() + ":" + setting.getSerializedValue());
            }
        }
        Files.move(tempFile.toPath(), XaeroPlus.configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    public static synchronized void loadXPSettings() {
        try {
            if (!XaeroPlus.configFile.exists()) return;
            loadXPSettingsFromFile(XaeroPlus.configFile);
        } catch (final Throwable e) {
            XaeroPlus.LOGGER.error("Error loading XaeroPlus settings", e);
        }
    }

    public static synchronized void loadXPSettingsFromFile(File file) throws IOException {
        try(BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String s;
            while ((s = reader.readLine()) != null) {
                String[] args = s.split(":");
                if (args.length != 2) {
                    XaeroPlus.LOGGER.warn("Invalid setting line: {}", s);
                    continue;
                }
                var setting = Settings.REGISTRY.getSettingByName(args[0]);
                if (setting == null) {
                    XaeroPlus.LOGGER.warn("Setting not found: {}", args[0]);
                    continue;
                }
                setting.deserializeValue(args[1]);
            }
        }
    }

    public static void getClientBooleanValue(String enumString, CallbackInfoReturnable<Boolean> cir) {
        var setting = Settings.REGISTRY.getSettingByName(enumString);
        if (setting instanceof BooleanSetting booleanSetting) {
            cir.setReturnValue(booleanSetting.get());
        }
    }

    // boolean or enum...
    public static void setOptionValue(String enumString, final Object value) {
        var setting = Settings.REGISTRY.getSettingByName(enumString);
        if (setting instanceof BooleanSetting booleanSetting && value instanceof Boolean) {
            booleanSetting.setValue((Boolean) value);
        } else if (setting instanceof EnumSetting enumSetting && value instanceof Integer) {
            enumSetting.setValueIndex((Integer) value);
        }
    }

    // boolean or enum...
    public static void getOptionValue(final String enumString, final CallbackInfoReturnable<Object> cir) {
        var setting = Settings.REGISTRY.getSettingByName(enumString);
        if (setting instanceof BooleanSetting booleanSetting) {
            cir.setReturnValue(booleanSetting.get());
        } else if (setting instanceof EnumSetting enumSetting) {
            cir.setReturnValue(enumSetting.getValueIndex());
        }
    }

    public static void setOptionDoubleValue(String enumString, double f) {
        var setting = Settings.REGISTRY.getSettingByName(enumString);
        if (setting instanceof DoubleSetting doubleSetting) {
            doubleSetting.setValue(f);
        }
    }

    public static void getOptionDoubleValue(String enumString, CallbackInfoReturnable<Double> cir) {
        var setting = Settings.REGISTRY.getSettingByName(enumString);
        if (setting instanceof DoubleSetting doubleSetting) {
            cir.setReturnValue(doubleSetting.get());
        }
    }

    public static void getOptionValueName(final String enumString, final CallbackInfoReturnable<String> cir) {
        var setting = Settings.REGISTRY.getSettingByName(enumString);
        if (setting instanceof EnumSetting enumSetting) {
            cir.setReturnValue(enumSetting.get() instanceof TranslatableSettingEnum
                                   ? ((TranslatableSettingEnum) enumSetting.get()).getTranslatedName()
                                   : enumSetting.get().toString());
        }
    }

    public static void getSliderOptionText(final String enumString, final CallbackInfoReturnable<String> cir) {
        var setting = Settings.REGISTRY.getSettingByName(enumString);
        if (setting == null) return;
        var prefix = setting.getTranslatedName() + ": ";
        if (setting instanceof DoubleSetting doubleSetting) {
            cir.setReturnValue(prefix + String.format("%.2f", doubleSetting.get()));
        } else if (setting instanceof EnumSetting enumSetting) {
            cir.setReturnValue(prefix + (enumSetting.get() instanceof TranslatableSettingEnum
                                   ? ((TranslatableSettingEnum) enumSetting.get()).getTranslatedName()
                                   : enumSetting.get().toString()));
        }
    }
}
