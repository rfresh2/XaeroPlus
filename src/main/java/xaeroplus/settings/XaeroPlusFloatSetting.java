package xaeroplus.settings;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import xaeroplus.XaeroPlus;
import xaeroplus.settings.XaeroPlusSettingsReflectionHax.SettingLocation;

import java.util.function.Consumer;

import static java.util.Objects.nonNull;

public class XaeroPlusFloatSetting extends XaeroPlusSetting {
    private final float valueMin;
    private final float valueMax;
    private final float valueStep;

    private float value;
    private Consumer<Float> settingChangeConsumer;

    private XaeroPlusFloatSetting(final String settingName,
                                  final float valueMin, final float valueMax, final float valueStep,
                                  final float defaultValue,
                                  final Text tooltip,
                                  final Consumer<Float> settingChangeConsumer,
                                  final KeyBinding keyBinding) {
        super(settingName, tooltip, keyBinding);
        this.valueMin = valueMin;
        this.valueMax = valueMax;
        this.valueStep = valueStep;
        this.value = defaultValue;
        this.settingChangeConsumer = settingChangeConsumer;
    }

    public static XaeroPlusFloatSetting create(String settingName, float valueMin, float valueMax, float valueStep,
                                               String tooltip, float defaultValue, SettingLocation settingLocation) {
        final XaeroPlusFloatSetting setting = new XaeroPlusFloatSetting(
                SETTING_PREFIX + settingName,
                valueMin, valueMax, valueStep, defaultValue,
                Text.of(defaultValueStr(settingName, defaultValue) + tooltip),
                null,
                null);
        settingLocation.getSettingsList().add(setting);
        return setting;
    }

    public static XaeroPlusFloatSetting create(String settingName, float valueMin, float valueMax, float valueStep,
                                               String tooltip, Consumer<Float> changeConsumer, float defaultValue,
                                               SettingLocation settingLocation) {
        final XaeroPlusFloatSetting setting = new XaeroPlusFloatSetting(
                SETTING_PREFIX + settingName,
                valueMin, valueMax, valueStep, defaultValue,
                Text.of(defaultValueStr(settingName, defaultValue) + tooltip),
                changeConsumer,
                null);
        settingLocation.getSettingsList().add(setting);
        return setting;
    }

    public float getValueMin() {
        return valueMin;
    }

    public float getValueMax() {
        return valueMax;
    }

    public float getValueStep() {
        return valueStep;
    }

    public float getValue() {
        return value;
    }

    public void setValue(final float value) {
        this.value = value;
        if (nonNull(getSettingChangeConsumer())) {
            try {
                getSettingChangeConsumer().accept(value);
            } catch (final Exception e) {
                XaeroPlus.LOGGER.warn("Error applying setting change consumer for {}", getSettingName(), e);
            }
        }
    }

    public Consumer<Float> getSettingChangeConsumer() {
        return settingChangeConsumer;
    }

    public void setSettingChangeConsumer(final Consumer<Float> settingChangeConsumer) {
        this.settingChangeConsumer = settingChangeConsumer;
    }
}
