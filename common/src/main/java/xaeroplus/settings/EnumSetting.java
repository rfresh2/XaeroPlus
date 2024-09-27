package xaeroplus.settings;

import net.minecraft.client.KeyMapping;
import org.apache.commons.lang3.ArrayUtils;
import xaero.common.settings.ModOptions;
import xaero.map.gui.CursorBox;
import xaeroplus.XaeroPlus;
import xaeroplus.mixin.client.AccessorMinimapModOptions;
import xaeroplus.mixin.client.AccessorWorldMapModOptions;

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
    public ModOptions toMinimapModOptions() {
        return AccessorMinimapModOptions.createEnumSetting(
            getSettingName(),
            0,
            getIndexMax(),
            new xaero.common.graphics.CursorBox(getTooltipTranslationKey()),
            isIngameOnly()
        );
    }

    @Override
    public xaero.map.settings.ModOptions toWorldMapModOptions() {
        return AccessorWorldMapModOptions.createEnumSetting(
            getSettingName(),
            getIndexMax() + 1,
            new CursorBox(getTooltipTranslationKey()),
            isIngameOnly(),
            isRequiresMinimap(),
            false
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
