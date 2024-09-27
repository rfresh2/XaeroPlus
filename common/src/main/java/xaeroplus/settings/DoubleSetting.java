package xaeroplus.settings;

import net.minecraft.client.KeyMapping;
import xaero.common.settings.ModOptions;
import xaero.map.gui.CursorBox;
import xaeroplus.XaeroPlus;
import xaeroplus.mixin.client.AccessorMinimapModOptions;
import xaeroplus.mixin.client.AccessorWorldMapModOptions;

import java.util.function.BooleanSupplier;
import java.util.function.DoubleConsumer;

import static java.util.Objects.nonNull;

public class DoubleSetting extends XaeroPlusSetting {
    private final double valueMin;
    private final double valueMax;
    private final double valueStep;
    private double value;
    private DoubleConsumer settingChangeConsumer;

    private DoubleSetting(final String settingName,
                          final String settingNameTranslationKey,
                          final String tooltipTranslationKey,
                          final KeyMapping keyBinding,
                          final double valueMin,
                          final double valueMax,
                          final double valueStep,
                          final double defaultValue,
                          final DoubleConsumer settingChangeConsumer,
                          final BooleanSupplier visibilitySupplier) {
        super(settingName, settingNameTranslationKey, tooltipTranslationKey, keyBinding, visibilitySupplier);
        this.valueMin = valueMin;
        this.valueMax = valueMax;
        this.valueStep = valueStep;
        this.value = defaultValue;
        this.settingChangeConsumer = settingChangeConsumer;
    }

    public static DoubleSetting create(String settingName,
                                       String settingNameTranslationKey,
                                       double valueMin,
                                       double valueMax,
                                       double valueStep,
                                       double defaultValue) {
        return new DoubleSetting(
            SETTING_PREFIX + settingName,
            settingNameTranslationKey,
            buildTooltipTranslationKey(settingNameTranslationKey),
            null,
            valueMin, valueMax, valueStep, defaultValue, null, null
        );
    }

    public static DoubleSetting create(String settingName,
                                       String settingNameTranslationKey,
                                       double valueMin,
                                       double valueMax,
                                       double valueStep,
                                       double defaultValue,
                                       DoubleConsumer changeConsumer) {
        return new DoubleSetting(
            SETTING_PREFIX + settingName,
            settingNameTranslationKey,
            buildTooltipTranslationKey(settingNameTranslationKey),
            null,
            valueMin, valueMax, valueStep, defaultValue, changeConsumer, null
        );
    }

    public static DoubleSetting create(String settingName,
                                       String settingNameTranslationKey,
                                       double valueMin,
                                       double valueMax,
                                       double valueStep,
                                       double defaultValue,
                                       DoubleConsumer changeConsumer,
                                       BooleanSupplier visibilitySupplier) {
        return new DoubleSetting(
            SETTING_PREFIX + settingName,
            settingNameTranslationKey,
            buildTooltipTranslationKey(settingNameTranslationKey),
            null,
            valueMin, valueMax, valueStep, defaultValue, changeConsumer, visibilitySupplier
        );
    }

    @Override
    public String getSerializedValue() {
        return Double.toString(value);
    }

    @Override
    public void deserializeValue(String value) {
        var f = Double.parseDouble(value);
        if (f != get()) setValue(f);
    }

    @Override
    public ModOptions toMinimapModOptions() {
        return AccessorMinimapModOptions.createDoubleSetting(
            getSettingName(),
            getValueMin(),
            getValueMax(),
            (float) getValueStep(),
            new xaero.common.graphics.CursorBox(getTooltipTranslationKey()),
            isIngameOnly()
        );
    }

    @Override
    public xaero.map.settings.ModOptions toWorldMapModOptions() {
        return AccessorWorldMapModOptions.createDoubleSetting(
            getSettingName(),
            getValueMin(),
            getValueMax(),
            getValueStep(),
            new CursorBox(getTooltipTranslationKey()),
            isIngameOnly(),
            isRequiresMinimap(),
            false
        );
    }

    public double getValueMin() {
        return valueMin;
    }

    public double getValueMax() {
        return valueMax;
    }

    public double getValueStep() {
        return valueStep;
    }

    public double get() {
        return value;
    }

    public int getAsInt() {
        return (int) value;
    }

    public void setValue(final double value) {
        this.value = value;
        if (nonNull(getSettingChangeConsumer())) {
            try {
                getSettingChangeConsumer().accept(value);
            } catch (final Exception e) {
                XaeroPlus.LOGGER.warn("Error applying setting change consumer for {}", getSettingName(), e);
            }
        }
    }

    public DoubleConsumer getSettingChangeConsumer() {
        return settingChangeConsumer;
    }

    public void setSettingChangeConsumer(final DoubleConsumer settingChangeConsumer) {
        this.settingChangeConsumer = settingChangeConsumer;
    }
    public void init() {
        if (nonNull(settingChangeConsumer)) {
            settingChangeConsumer.accept(value);
        }
    }
}
