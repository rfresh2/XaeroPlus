package xaeroplus.settings;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import xaero.lib.common.gui.widget.TooltipInfo;
import xaeroplus.XaeroPlus;
import xaeroplus.feature.extensions.XaeroPlusSettingEntry;

import java.util.function.BooleanSupplier;
import java.util.function.DoubleConsumer;

import static java.util.Objects.nonNull;

public class DoubleSetting extends XaeroPlusSetting {
    private final double valueMin;
    private final double valueMax;
    private final double valueStep;
    private double value;
    private DoubleConsumer settingChangeConsumer;

    private DoubleSetting(
        final String settingName,
        final String settingNameTranslationKey,
        final String tooltipTranslationKey,
        final KeyBinding keyBinding,
        final BooleanSupplier visibilitySupplier,
        final double valueMax,
        final double defaultValue,
        final double valueMin,
        final DoubleConsumer settingChangeConsumer,
        final double valueStep
    ) {
        super(settingName, settingNameTranslationKey, tooltipTranslationKey, keyBinding, visibilitySupplier);
        this.valueMin = valueMin;
        this.valueMax = valueMax;
        this.valueStep = valueStep;
        this.value = defaultValue;
        this.settingChangeConsumer = settingChangeConsumer;
    }

    public static DoubleSetting create(
        String settingName,
        String settingNameTranslationKey,
        double valueMin,
        double valueMax,
        double valueStep,
        String tooltipTranslationKey,
        double defaultValue
    ) {
        return new DoubleSetting(
            SETTING_PREFIX + settingName,
            settingNameTranslationKey,
            tooltipTranslationKey,
            null,
            null,
            valueMax,
            defaultValue,
            valueMin,
            null,
            valueStep
        );
    }

    public static DoubleSetting create(
        String settingName,
        String settingNameTranslationKey,
        double valueMin,
        double valueMax,
        double valueStep,
        String tooltipTranslationKey,
        DoubleConsumer changeConsumer,
        double defaultValue
    ) {
        return new DoubleSetting(
            SETTING_PREFIX + settingName,
            settingNameTranslationKey,
            //todo: inject this somehow?
            // new TextComponentString(defaultValueStr(settingName, defaultValue) + tooltip),
            tooltipTranslationKey,
            null,
            null,
            valueMax,
            defaultValue,
            valueMin,
            changeConsumer,
            valueStep
        );
    }

    public static DoubleSetting create(
        String settingName,
        String settingNameTranslationKey,
        double valueMin,
        double valueMax,
        double valueStep,
        String tooltipTranslationKey,
        BooleanSupplier visibilitySupplier,
        DoubleConsumer changeConsumer,
        double defaultValue
    ) {
        return new DoubleSetting(
            SETTING_PREFIX + settingName,
            settingNameTranslationKey,
            tooltipTranslationKey,
            null,
            visibilitySupplier,
            valueMax,
            defaultValue,
            valueMin,
            changeConsumer,
            valueStep
        );
    }

    @Override
    public String getSerializedValue() {
        return Double.toString(value);
    }

    @Override
    public void deserializeValue(String value) {
        double f = Double.parseDouble(value);
        if (f != getValue()) setValue(f);
    }

    @Override
    public XaeroPlusSettingEntry<?> toXaeroSettingEntry() {
        int numIndeces = (int) ((valueMax - valueMin) / valueStep);
        return new XaeroPlusSettingEntry<Double>(
            this,
            new TextComponentString(getTranslatedName()),
            new TooltipInfo(getTooltipTranslationKey()),
            true,
            this::getValue,
            0,
            numIndeces,
            v -> MathHelper.clamp(valueMin + (v * valueStep), valueMin, valueMax),
            v -> new TextComponentString(String.format("%.2f", v)),
            (v1, v2) -> {
                setValue(v2);
                SettingHooks.saveSettings();
                Minecraft.getMinecraft().displayGuiScreen(Minecraft.getMinecraft().currentScreen);
            },
            this::isVisible
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

    public double getValue() {
        return value;
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
