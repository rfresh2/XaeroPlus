package xaeroplus.settings;

import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;

import java.util.function.Supplier;


public abstract class XaeroPlusSetting {
    public static final String SETTING_PREFIX = "[XP] ";
    private final String settingName; // enumString. setting name that is used in the config file
    private final String settingNameTranslationKey;
    private String tooltipTranslationKey;
    private static boolean ingameOnly = false;
    private static boolean requiresMinimap = false;
    private KeyBinding keyBinding;
    private Supplier<Boolean> visibilitySupplier;

    public XaeroPlusSetting(String settingName,
                            String settingNameTranslationKey,
                            String tooltipTranslationKey, // nullable
                            KeyBinding keyBinding, // nullable
                            Supplier<Boolean> visibilitySupplier
    ) {
        this.settingName = settingName;
        this.settingNameTranslationKey = settingNameTranslationKey;
        this.tooltipTranslationKey = tooltipTranslationKey;
        this.keyBinding = keyBinding;
        this.visibilitySupplier = visibilitySupplier;
    }

    protected static String defaultValueStr(final String settingName, final Object defaultVal) {
        return settingName + " \\n    Default: " + defaultVal + " \\n ";
    }

    public String getSettingName() {
        return settingName;
    }
    public String getSettingNameTranslationKey() {
        return settingNameTranslationKey;
    }
    public String getTranslatedName() {
        return SETTING_PREFIX + I18n.format(getSettingNameTranslationKey());
    }
    public String getTooltipTranslationKey() {
        return tooltipTranslationKey;
    }
    public boolean isIngameOnly() {
        return ingameOnly;
    }
    public boolean isRequiresMinimap() {
        return requiresMinimap;
    }
    public KeyBinding getKeyBinding() {
        return keyBinding;
    }
    public void setKeyBinding(KeyBinding keyBinding) {
        this.keyBinding = keyBinding;
    }
    public boolean isVisible() {
        if (visibilitySupplier != null) {
            return visibilitySupplier.get();
        } else {
            return true;
        }
    }
    public abstract void init();
}
