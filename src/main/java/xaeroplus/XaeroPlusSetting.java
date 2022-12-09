package xaeroplus;

import xaero.map.gui.CursorBox;

import java.util.Objects;

public class XaeroPlusSetting {
    public static final String SETTING_PREFIX = "[XaeroPlus] ";
    private final boolean isFloatSetting; // enumFloat
    private final boolean isBooleanSetting; // enumBoolean
    private final String settingName; // enumString
    private float valueMin;
    private float valueMax;
    private float valueStep;
    private CursorBox tooltip;
    private static boolean ingameOnly = false;
    private static boolean requiresMinimap = false;
    private static boolean switchScreen = false;
    private float floatSettingValue;
    private boolean booleanSettingValue;

    public XaeroPlusSetting(String settingName, boolean isFloatSetting, boolean isBooleanSetting,
                            float valueMin, float valueMax, float valueStep, // only relevant for float settings
                            CursorBox tooltip // nullable
    ) {
        this.isFloatSetting = isFloatSetting;
        this.isBooleanSetting = isBooleanSetting;
        this.settingName = settingName;
        this.valueMin = valueMin;
        this.valueMax = valueMax;
        this.valueStep = valueStep;
        this.tooltip = tooltip;
    }

    public static XaeroPlusSetting createFloatSetting(String settingName, float valueMin, float valueMax, float valueStep, CursorBox tooltip, float defaultValue) {
        final XaeroPlusSetting xaeroPlusSetting = new XaeroPlusSetting(SETTING_PREFIX + settingName, true,
                false, valueMin, valueMax, valueStep, tooltip);
        xaeroPlusSetting.setFloatSettingValue(defaultValue);
        return xaeroPlusSetting;
    }

    public static XaeroPlusSetting createBooleanSetting(String settingName, CursorBox tooltip, boolean defaultValue) {
        final XaeroPlusSetting xaeroPlusSetting = new XaeroPlusSetting(SETTING_PREFIX + settingName, false,
                true, 0, 0, 0, tooltip);
        xaeroPlusSetting.setBooleanSettingValue(defaultValue);
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

    public CursorBox getTooltip() {
        return tooltip;
    }

    public void setTooltip(CursorBox tooltip) {
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
    }

    public boolean getBooleanSettingValue() {
        return booleanSettingValue;
    }

    public void setBooleanSettingValue(boolean booleanSettingValue) {
        this.booleanSettingValue = booleanSettingValue;
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
