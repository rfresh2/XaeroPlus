package xaeroplus.settings;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import xaero.map.gui.CursorBox;
import xaeroplus.XaeroPlus;

import java.util.Objects;
import java.util.function.Consumer;

import static java.util.Objects.nonNull;

public class XaeroPlusSetting {
    public static final String SETTING_PREFIX = "[XP] ";
    private final boolean isFloatSetting; // enumFloat
    private final boolean isBooleanSetting; // enumBoolean
    private final String settingName; // enumString
    private float valueMin;
    private float valueMax;
    private float valueStep;
    private ITextComponent tooltip;
    private static boolean ingameOnly = false;
    private static boolean requiresMinimap = false;
    private static boolean switchScreen = false;
    private float floatSettingValue;
    private boolean booleanSettingValue;
    private Consumer<Float> floatSettingChangeConsumer;
    private Consumer<Boolean> booleanSettingChangeConsumer;
    private KeyBinding keyBinding;

    public XaeroPlusSetting(String settingName, boolean isFloatSetting, boolean isBooleanSetting,
                            float valueMin, float valueMax, float valueStep, // only relevant for float settings
                            ITextComponent tooltip, // nullable
                            KeyBinding keyBinding // nullable
    ) {
        this.isFloatSetting = isFloatSetting;
        this.isBooleanSetting = isBooleanSetting;
        this.settingName = settingName;
        this.valueMin = valueMin;
        this.valueMax = valueMax;
        this.valueStep = valueStep;
        this.tooltip = tooltip;
        this.keyBinding = keyBinding;
    }

    private static String defaultValueStr(final String settingName, final Object defaultVal) {
        return settingName + " \\n    Default: " + defaultVal + " \\n ";
    }

    public static XaeroPlusSetting createFloatSetting(String settingName, float valueMin, float valueMax, float valueStep, String tooltip, float defaultValue) {
        final XaeroPlusSetting xaeroPlusSetting = new XaeroPlusSetting(SETTING_PREFIX + settingName, true,
                false, valueMin, valueMax, valueStep, new TextComponentString(defaultValueStr(settingName, defaultValue) + tooltip), null);
        xaeroPlusSetting.setFloatSettingValue(defaultValue);
        return xaeroPlusSetting;
    }

    public static XaeroPlusSetting createFloatSetting(String settingName, float valueMin, float valueMax, float valueStep, String tooltip, Consumer<Float> changeConsumer, float defaultValue) {
        final XaeroPlusSetting xaeroPlusSetting = createFloatSetting(settingName, valueMin, valueMax, valueStep, tooltip, defaultValue);
        xaeroPlusSetting.setFloatSettingChangeConsumer(changeConsumer);
        return xaeroPlusSetting;
    }

    public static XaeroPlusSetting createBooleanSetting(String settingName, String tooltip, boolean defaultValue) {
        final XaeroPlusSetting xaeroPlusSetting = new XaeroPlusSetting(SETTING_PREFIX + settingName, false,
                true, 0, 0, 0, new TextComponentString(defaultValueStr(settingName, defaultValue) + tooltip), null);
        xaeroPlusSetting.setBooleanSettingValue(defaultValue);
        xaeroPlusSetting.setKeyBinding(new KeyBinding(settingName, 0, "XaeroPlus"));
        return xaeroPlusSetting;
    }

    public static XaeroPlusSetting createBooleanSetting(String settingName, String tooltip, Consumer<Boolean> settingChangeConsumer, boolean defaultValue) {
        final XaeroPlusSetting xaeroPlusSetting = createBooleanSetting(settingName, tooltip, defaultValue);
        xaeroPlusSetting.setBooleanSettingChangeConsumer(settingChangeConsumer);
        return xaeroPlusSetting;
    }

    public boolean isFloatSetting() {
        return isFloatSetting;
    }

    public boolean isBooleanSetting() {
        return isBooleanSetting;
    }

    public String getSettingName() {
        return settingName;
    }

    public float getValueMin() {
        return valueMin;
    }

    public void setValueMin(float valueMin) {
        this.valueMin = valueMin;
    }

    public float getValueMax() {
        return valueMax;
    }

    public void setValueMax(float valueMax) {
        this.valueMax = valueMax;
    }

    public float getValueStep() {
        return valueStep;
    }

    public void setValueStep(float valueStep) {
        this.valueStep = valueStep;
    }

    public ITextComponent getTooltip() {
        return tooltip;
    }

    public void setTooltip(ITextComponent tooltip) {
        this.tooltip = tooltip;
    }

    public boolean isIngameOnly() {
        return ingameOnly;
    }

    public void setIngameOnly(boolean ingameOnly) {
        XaeroPlusSetting.ingameOnly = ingameOnly;
    }

    public boolean isRequiresMinimap() {
        return requiresMinimap;
    }

    public void setRequiresMinimap(boolean requiresMinimap) {
        XaeroPlusSetting.requiresMinimap = requiresMinimap;
    }

    public boolean isSwitchScreen() {
        return switchScreen;
    }

    public void setSwitchScreen(boolean switchScreen) {
        XaeroPlusSetting.switchScreen = switchScreen;
    }

    public float getFloatSettingValue() {
        return floatSettingValue;
    }

    public void setFloatSettingValue(float floatSettingValue) {
        this.floatSettingValue = floatSettingValue;
        if (nonNull(getFloatSettingChangeConsumer())) {
            try {
                getFloatSettingChangeConsumer().accept(floatSettingValue);
            } catch (final Exception e) {
                XaeroPlus.LOGGER.warn("Error applying setting change consumer for {}", this.settingName, e);
            }
        }
    }

    public boolean getBooleanSettingValue() {
        return booleanSettingValue;
    }

    public void setBooleanSettingValue(boolean booleanSettingValue) {
        this.booleanSettingValue = booleanSettingValue;
        if (nonNull(getBooleanSettingChangeConsumer())) {
            try {
                getBooleanSettingChangeConsumer().accept(booleanSettingValue);
            } catch (final Exception e) {
                XaeroPlus.LOGGER.warn("Error applying setting change consumer for {}", this.settingName, e);
            }
        }
    }

    public Consumer<Float> getFloatSettingChangeConsumer() {
        return floatSettingChangeConsumer;
    }

    public void setFloatSettingChangeConsumer(Consumer<Float> floatSettingChangeConsumer) {
        this.floatSettingChangeConsumer = floatSettingChangeConsumer;
    }

    public Consumer<Boolean> getBooleanSettingChangeConsumer() {
        return booleanSettingChangeConsumer;
    }

    public void setBooleanSettingChangeConsumer(Consumer<Boolean> booleanSettingChangeConsumer) {
        this.booleanSettingChangeConsumer = booleanSettingChangeConsumer;
    }

    public KeyBinding getKeyBinding() {
        return keyBinding;
    }

    public void setKeyBinding(KeyBinding keyBinding) {
        this.keyBinding = keyBinding;
    }

    @Override
    public String toString() {
        return "XaeroPlusSetting{" +
                "isFloatSetting=" + isFloatSetting +
                ", isBooleanSetting=" + isBooleanSetting +
                ", settingName='" + settingName + '\'' +
                ", valueMin=" + valueMin +
                ", valueMax=" + valueMax +
                ", valueStep=" + valueStep +
                ", tooltip=" + tooltip +
                ", floatSettingValue=" + floatSettingValue +
                ", booleanSettingValue=" + booleanSettingValue +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        XaeroPlusSetting that = (XaeroPlusSetting) o;
        return isFloatSetting() == that.isFloatSetting() && isBooleanSetting() == that.isBooleanSetting() && Float.compare(that.getValueMin(), getValueMin()) == 0 && Float.compare(that.getValueMax(), getValueMax()) == 0 && Float.compare(that.getValueStep(), getValueStep()) == 0 && Float.compare(that.getFloatSettingValue(), getFloatSettingValue()) == 0 && getBooleanSettingValue() == that.getBooleanSettingValue() && getSettingName().equals(that.getSettingName()) && Objects.equals(getTooltip(), that.getTooltip());
    }

    @Override
    public int hashCode() {
        return Objects.hash(isFloatSetting(), isBooleanSetting(), getSettingName(), getValueMin(), getValueMax(), getValueStep(), getTooltip(), getFloatSettingValue(), getBooleanSettingValue());
    }
}
