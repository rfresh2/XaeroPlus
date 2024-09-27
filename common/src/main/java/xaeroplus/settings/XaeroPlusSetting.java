package xaeroplus.settings;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.resources.language.I18n;

import java.util.function.BooleanSupplier;

public abstract class XaeroPlusSetting {
    public static final String SETTING_PREFIX = "[XP] ";
    private final String settingName; // enumString. setting name that is used in the config file
    private final String settingNameTranslationKey;
    private String tooltipTranslationKey;
    private static boolean ingameOnly = false;
    private static boolean requiresMinimap = false;
    private KeyMapping keyBinding;
    private BooleanSupplier visibilitySupplier;


    public XaeroPlusSetting(String settingName,
                            String settingNameTranslationKey,
                            String tooltipTranslationKey, // nullable
                            KeyMapping keyBinding, // nullable
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

    public abstract xaero.common.settings.ModOptions toMinimapModOptions();

    public abstract xaero.map.settings.ModOptions toWorldMapModOptions();

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

    public static String buildTooltipTranslationKey(String baseKey) {
        return baseKey + ".tooltip";
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
            return visibilitySupplier.getAsBoolean();
        } else {
            return true;
        }
    }

    public xaero.common.gui.ConfigSettingEntry toMinimapConfigSettingEntry() {
        return new xaero.common.gui.ConfigSettingEntry(toMinimapModOptions());
    }

    public xaero.map.gui.ConfigSettingEntry toWorldmapConfigSettingEntry() {
        return new xaero.map.gui.ConfigSettingEntry(toWorldMapModOptions());
    }
}
