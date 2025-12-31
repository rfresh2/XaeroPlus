package xaeroplus.settings;

import xaeroplus.XaeroPlus;
import xaeroplus.util.FileUtil;

import java.io.*;
import java.util.Comparator;

public class SettingHooks {
    public static void saveSettings() {
        try {
            saveXPSettings();
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Failed saving settings", e);
        }
    }

    public static synchronized void saveXPSettings() throws IOException {
        FileUtil.safeSave(XaeroPlus.configFile, w -> {
            try (PrintWriter writer = new PrintWriter(w)) {
                var allSettings = Settings.REGISTRY.getAllSettings().stream().sorted(Comparator.comparing(XaeroPlusSetting::getSettingName)).toList();
                for (int i = 0; i < allSettings.size(); i++) {
                    final XaeroPlusSetting setting = allSettings.get(i);
                    writer.println(setting.getSettingName() + ":" + setting.getSerializedValue());
                }
            }
        });
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
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String s;
            while ((s = reader.readLine()) != null) {
                int colonIndex = s.indexOf(':');
                if (colonIndex == -1) continue;
                String settingName = s.substring(0, colonIndex);
                if (settingName.isBlank()) continue;
                String settingValue = s.substring(colonIndex + 1);
                var setting = Settings.REGISTRY.getSettingByName(settingName);
                if (setting == null) {
                    XaeroPlus.LOGGER.warn("Setting not found: {}", settingName);
                    continue;
                }
                setting.deserializeValue(settingValue);
            }
        }
    }
}
