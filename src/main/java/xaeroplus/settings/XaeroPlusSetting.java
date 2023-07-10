package xaeroplus.settings;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.text.ITextComponent;

import java.util.function.Supplier;


public abstract class XaeroPlusSetting {
    public static final String SETTING_PREFIX = "[XP] ";
    private final String settingName; // enumString
    private ITextComponent tooltip;
    private static boolean ingameOnly = false;
    private static boolean requiresMinimap = false;
    private static boolean switchScreen = false;
    private KeyBinding keyBinding;
    private Supplier<Boolean> visibilitySupplier;

    public XaeroPlusSetting(String settingName,
                            ITextComponent tooltip, // nullable
                            KeyBinding keyBinding, // nullable
                            Supplier<Boolean> visibilitySupplier
    ) {
        this.settingName = settingName;
        this.tooltip = tooltip;
        this.keyBinding = keyBinding;
        this.visibilitySupplier = visibilitySupplier;
    }

    protected static String defaultValueStr(final String settingName, final Object defaultVal) {
        return settingName + " \\n    Default: " + defaultVal + " \\n ";
    }

    public String getSettingName() {
        return settingName;
    }

    public ITextComponent getTooltip() {
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
