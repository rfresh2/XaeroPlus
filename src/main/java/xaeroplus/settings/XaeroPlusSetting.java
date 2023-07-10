package xaeroplus.settings;


import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;

import java.util.function.Supplier;

public abstract class XaeroPlusSetting {
    public static final String SETTING_PREFIX = "[XP] ";
    private final String settingName; // enumString
    private Text tooltip;
    private static boolean ingameOnly = false;
    private static boolean requiresMinimap = false;
    private KeyBinding keyBinding;
    private Supplier<Boolean> visibilitySupplier;


    public XaeroPlusSetting(String settingName,
                            Text tooltip, // nullable
                            KeyBinding keyBinding, // nullable
                            Supplier<Boolean> visibilitySupplier // nullable
    ) {
        this.settingName = settingName;
        this.tooltip = tooltip;
        this.keyBinding = keyBinding;
        this.visibilitySupplier = visibilitySupplier;
    }

    protected static String defaultValueStr(final String settingName, final Object defaultVal) {
        return settingName + " \n    Default: " + defaultVal + " \n ";
    }

    public String getSettingName() {
        return settingName;
    }

    public Text getTooltip() {
        return tooltip;
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
