package xaeroplus.settings;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.ArrayUtils;
import xaero.lib.common.gui.widget.TooltipInfo;
import xaeroplus.XaeroPlus;
import xaeroplus.feature.extensions.XaeroPlusSettingEntry;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import static java.util.Objects.nonNull;

public class EnumSetting<T extends Enum<T>> extends XaeroPlusSetting {
    private final T[] enumValues;
    private T value;
    private Consumer<T> settingChangeConsumer;

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

    public static <E extends Enum<E>> EnumSetting<E> create(
        String settingName,
        String settingNameTranslationKey,
        E[] values,
        E defaultValue) {
        return new EnumSetting<>(
            SETTING_PREFIX + settingName,
            settingNameTranslationKey,
            buildTooltipTranslationKey(settingNameTranslationKey),
            null,
            values, defaultValue, null, null
        );
    }

    public static <E extends Enum<E>> EnumSetting<E> create(
        String settingName,
        String settingNameTranslationKey,
        E[] values,
        E defaultValue,
        Consumer<E> settingChangeConsumer) {
        return new EnumSetting<>(
            SETTING_PREFIX + settingName,
            settingNameTranslationKey,
            buildTooltipTranslationKey(settingNameTranslationKey),
            null,
            values,
            defaultValue,
            settingChangeConsumer,
            null
        );
    }

    public static <E extends Enum<E>> EnumSetting<E> create(
        String settingName,
        String settingNameTranslationKey,
        E[] values,
        E defaultValue,
        Consumer<E> settingChangeConsumer,
        BooleanSupplier visibilitySupplier) {
        return new EnumSetting<>(
            SETTING_PREFIX + settingName,
            settingNameTranslationKey,
            buildTooltipTranslationKey(settingNameTranslationKey),
            null,
            values,
            defaultValue,
            settingChangeConsumer,
            visibilitySupplier
        );
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
    public XaeroPlusSettingEntry<?> toXaeroSettingEntry() {
        return new XaeroPlusSettingEntry<T>(
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
                Minecraft.getInstance().setScreen(Minecraft.getInstance().screen);
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
