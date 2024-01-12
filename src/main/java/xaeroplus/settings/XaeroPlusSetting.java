package xaeroplus.settings;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.resources.language.I18n;

import java.util.function.Supplier;

public abstract class XaeroPlusSetting {
    public static final String SETTING_PREFIX = "[XP] ";
    private final String settingName; // enumString. setting name that is used in the config file
    private final String settingNameTranslationKey;
    private String tooltipTranslationKey;
    private static boolean ingameOnly = false;
    private static boolean requiresMinimap = false;
    private KeyMapping keyBinding;
    private Supplier<Boolean> visibilitySupplier;


    public XaeroPlusSetting(String settingName,
                            String settingNameTranslationKey,
                            String tooltipTranslationKey, // nullable
                            KeyMapping keyBinding, // nullable
                            Supplier<Boolean> visibilitySupplier // nullable
    ) {
        this.settingName = settingName;
        this.settingNameTranslationKey = settingNameTranslationKey;
        this.tooltipTranslationKey = tooltipTranslationKey;
        this.keyBinding = keyBinding;
        this.visibilitySupplier = visibilitySupplier;
    }

    protected static String defaultValueStr(final String settingName, final Object defaultVal) {
        return settingName + " \n    Default: " + defaultVal + " \n ";
    }

    public String getSettingName() {
        return settingName;
    }

    public String getSettingNameTranslationKey() {
        return settingNameTranslationKey;
    }

    public String getTranslatedName() {
        return SETTING_PREFIX + I18n.get(getSettingNameTranslationKey());
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
    public KeyMapping getKeyBinding() {
        return keyBinding;
    }
    public void setKeyBinding(KeyMapping keyBinding) {
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
