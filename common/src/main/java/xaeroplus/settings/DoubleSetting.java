package xaeroplus.settings;

import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import xaero.lib.common.gui.widget.TooltipInfo;
import xaeroplus.XaeroPlus;
import xaeroplus.feature.extensions.IXaeroPlusSettingEntry;
import xaeroplus.feature.extensions.XaeroPlusCustomSettingEntry;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleConsumer;

import static java.util.Objects.nonNull;

public class DoubleSetting extends XaeroPlusSetting {
    private final double valueMin;
    private final double valueMax;
    private final double valueStep;
    private double value;
    private DoubleConsumer settingChangeConsumer;

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String settingName;
        private String settingNameTranslationKey;
        private Double valueMin;
        private Double valueMax;
        private Double valueStep;
        private Double defaultValue;
        private DoubleConsumer settingChangeConsumer;
        private BooleanSupplier visibilitySupplier;

        private Builder() {}

        public Builder name(String settingName) {
            this.settingName = Objects.requireNonNull(settingName, "settingName");
            return this;
        }

        public Builder translationKey(String settingNameTranslationKey) {
            this.settingNameTranslationKey = Objects.requireNonNull(settingNameTranslationKey, "settingNameTranslationKey");
            return this;
        }

        public Builder range(double valueMin, double valueMax, double valueStep) {
            this.valueMin = valueMin;
            this.valueMax = valueMax;
            this.valueStep = valueStep;
            return this;
        }

        public Builder defaultValue(double defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder onChange(DoubleConsumer settingChangeConsumer) {
            this.settingChangeConsumer = Objects.requireNonNull(settingChangeConsumer, "settingChangeConsumer");
            return this;
        }

        public Builder visibleWhen(BooleanSupplier visibilitySupplier) {
            this.visibilitySupplier = Objects.requireNonNull(visibilitySupplier, "visibilitySupplier");
            return this;
        }

        public DoubleSetting build() {
            var name = Objects.requireNonNull(settingName, "settingName");
            var translationKey = Objects.requireNonNull(settingNameTranslationKey, "settingNameTranslationKey");
            return new DoubleSetting(
                SETTING_PREFIX + name,
                translationKey,
                buildTooltipTranslationKey(translationKey),
                null,
                Objects.requireNonNull(valueMin, "valueMin"),
                Objects.requireNonNull(valueMax, "valueMax"),
                Objects.requireNonNull(valueStep, "valueStep"),
                Objects.requireNonNull(defaultValue, "defaultValue"),
                settingChangeConsumer,
                visibilitySupplier
            );
        }
    }

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
    public IXaeroPlusSettingEntry toXaeroSettingEntry() {
        int numIndeces = (int) ((valueMax - valueMin) / valueStep);
        return new XaeroPlusCustomSettingEntry<Double>(
            this,
            Component.literal(getTranslatedName()),
            new TooltipInfo(getTooltipTranslationKey()),
            true,
            this::get,
            0,
            numIndeces,
            v -> Mth.clamp(valueMin + (v * valueStep), valueMin, valueMax),
            v -> Component.literal(String.format("%.2f", v)),
            (v1, v2) -> {
                setValue(v2);
                SettingHooks.saveSettings();
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
