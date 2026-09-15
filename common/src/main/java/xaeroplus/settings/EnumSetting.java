package xaeroplus.settings;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.ArrayUtils;
import xaero.lib.common.gui.widget.TooltipInfo;
import xaeroplus.XaeroPlus;
import xaeroplus.feature.extensions.IXaeroPlusSettingEntry;
import xaeroplus.feature.extensions.XaeroPlusCustomSettingEntry;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import static java.util.Objects.nonNull;

public class EnumSetting<T extends Enum<T>> extends XaeroPlusSetting {
    private final T[] enumValues;
    private T value;
    private Consumer<T> settingChangeConsumer;

    public static <E extends Enum<E>> Builder<E> builder() {
        return new Builder<>();
    }

    public static final class Builder<E extends Enum<E>> {
        private String settingName;
        private String settingNameTranslationKey;
        private E[] values;
        private E defaultValue;
        private Consumer<E> settingChangeConsumer;
        private BooleanSupplier visibilitySupplier;

        private Builder() {}

        public Builder<E> name(String settingName) {
            this.settingName = Objects.requireNonNull(settingName, "settingName");
            return this;
        }

        public Builder<E> translationKey(String settingNameTranslationKey) {
            this.settingNameTranslationKey = Objects.requireNonNull(settingNameTranslationKey, "settingNameTranslationKey");
            return this;
        }

        public Builder<E> values(E[] values) {
            this.values = Objects.requireNonNull(values, "values");
            return this;
        }

        public Builder<E> defaultValue(E defaultValue) {
            this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue");
            return this;
        }

        public Builder<E> onChange(Consumer<E> settingChangeConsumer) {
            this.settingChangeConsumer = Objects.requireNonNull(settingChangeConsumer, "settingChangeConsumer");
            return this;
        }

        public Builder<E> visibleWhen(BooleanSupplier visibilitySupplier) {
            this.visibilitySupplier = Objects.requireNonNull(visibilitySupplier, "visibilitySupplier");
            return this;
        }

        public EnumSetting<E> build() {
            var name = Objects.requireNonNull(settingName, "settingName");
            var translationKey = Objects.requireNonNull(settingNameTranslationKey, "settingNameTranslationKey");
            return new EnumSetting<>(
                SETTING_PREFIX + name,
                translationKey,
                buildTooltipTranslationKey(translationKey),
                null,
                Objects.requireNonNull(values, "values"),
                Objects.requireNonNull(defaultValue, "defaultValue"),
                settingChangeConsumer,
                visibilitySupplier
            );
        }
    }

    private EnumSetting(final String settingName,
                        final String settingNameTranslationKey,
                        final String tooltipTranslationKey,
                        final KeyMapping keyBinding,
                        final T[] enumValues,
                        final T defaultValue,
                        final Consumer<T> settingChangeConsumer,
                        final BooleanSupplier visibilitySupplier) {
        super(settingName, settingNameTranslationKey, tooltipTranslationKey, keyBinding, visibilitySupplier);
        this.enumValues = enumValues;
        this.value = defaultValue;
        this.settingChangeConsumer = settingChangeConsumer;
    }

    @Override
    public String getSerializedValue() {
        return Integer.toString(getValueIndex());
    }

    @Override
    public void deserializeValue(String value) {
        var index = Integer.parseInt(value);
        if (index != getValueIndex()) setValueIndex(index);
    }

    @Override
    public IXaeroPlusSettingEntry toXaeroSettingEntry() {
        return new XaeroPlusCustomSettingEntry<T>(
            this,
            Component.literal(getTranslatedName()),
            new TooltipInfo(getTooltipTranslationKey()),
            false,
            this::get,
            0,
            getIndexMax(),
            v -> getEnumValues()[v],
            v -> {
                if (v instanceof TranslatableSettingEnum translatableSettingEnum) {
                    return Component.translatable(translatableSettingEnum.getTranslationKey());
                }
                return Component.literal(v.toString());
            },
            (v1, v2) -> {
                setValue(v2);
                SettingHooks.saveSettings();
                Minecraft.getInstance().gui.setScreen(Minecraft.getInstance().gui.screen());
            },
            this::isVisible
        );
    }

    public T get() {
        return value;
    }

    public void setValue(T newVal) {
        this.value = newVal;
        if (nonNull(getSettingChangeConsumer())) {
            try {
                getSettingChangeConsumer().accept(newVal);
            } catch (final Exception e) {
                XaeroPlus.LOGGER.error("Error applying setting change consumer for setting: {}, value: {}", getSettingName(), newVal, e);
            }

        }
    }

    public int getValueIndex() {
        return ArrayUtils.indexOf(enumValues, get());
    }

    public void setValueIndex(final int index) {
        try {
            setValue(enumValues[index]);
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Unable to set enum value setting for {}, index {}", getSettingName(), index, e);
        }
    }

    public int getIndexMax() {
        return enumValues.length-1;
    }

    public void setSettingChangeConsumer(final Consumer<T> settingChangeConsumer) {
        this.settingChangeConsumer = settingChangeConsumer;
    }

    public Consumer<T> getSettingChangeConsumer() {
        return settingChangeConsumer;
    }

    public T[] getEnumValues() {
        return enumValues;
    }
    @Override
    public void init() {
        if (nonNull(settingChangeConsumer)) {
            settingChangeConsumer.accept(value);
        }
    }
}
