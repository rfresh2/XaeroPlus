package xaeroplus.settings;

import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import xaeroplus.feature.extensions.XaeroPlusSettingEntry;

import java.util.function.BooleanSupplier;

public abstract class XaeroPlusSetting {
    public static final String SETTING_PREFIX = "[XP] ";
    private final String settingName; // enumString. setting name that is used in the config file
    private final String settingNameTranslationKey;
    private String tooltipTranslationKey;
    private KeyBinding keyBinding;
    private BooleanSupplier visibilitySupplier;


    public XaeroPlusSetting(String settingName,
                            String settingNameTranslationKey,
                            String tooltipTranslationKey, // nullable
                            KeyBinding keyBinding, // nullable
                            BooleanSupplier visibilitySupplier // nullable
    ) {
        this.settingName = settingName;
        this.settingNameTranslationKey = settingNameTranslationKey;
        this.tooltipTranslationKey = tooltipTranslationKey;
        this.keyBinding = keyBinding;
        this.visibilitySupplier = visibilitySupplier;
    }

    // Called after setting is loaded from file for the first time
    public abstract void init();

    public abstract String getSerializedValue();

    public abstract void deserializeValue(String value);

    public abstract XaeroPlusSettingEntry<?> toXaeroSettingEntry();

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

    public static String buildTooltipTranslationKey(String baseKey) {
        return baseKey + ".tooltip";
    }

    public KeyBinding getKeyBinding() {
        return keyBinding;
    }

    public void setKeyBinding(KeyBinding keyBinding) {
        this.keyBinding = keyBinding;
    }

    public boolean isVisible() {
        if (visibilitySupplier != null) {
            return visibilitySupplier.getAsBoolean();
        } else {
            return true;
        }
    }
}
