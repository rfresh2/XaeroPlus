package xaeroplus.settings;

import xaeroplus.XaeroPlus;
import xaeroplus.feature.extensions.XaeroPlusSettingEntry;

import java.util.function.Consumer;

import static java.util.Objects.nonNull;

public class StringSetting extends XaeroPlusSetting {
    private String value;
    private Consumer<String> settingChangeConsumer;

    public StringSetting(
        final String settingName,
        final String settingNameTranslationKey,
        final String tooltipTranslationKey,
        final String defaultValue,
        final Consumer<String> settingChangeConsumer
    ) {
        super(settingName, settingNameTranslationKey, tooltipTranslationKey, null, () -> true);
        this.value = defaultValue;
        this.settingChangeConsumer = settingChangeConsumer;
    }

    public static StringSetting create(
        String settingName,
        String settingNameTranslationKey,
        String defaultValue,
        Consumer<String> settingChangeConsumer
    ) {
        return new StringSetting(
            SETTING_PREFIX + settingName,
            settingNameTranslationKey,
            buildTooltipTranslationKey(settingNameTranslationKey),
            defaultValue,
            settingChangeConsumer
        );
    }

    public static StringSetting create(
        String settingName,
        String settingNameTranslationKey,
        String defaultValue
    ) {
        return create(settingName, settingNameTranslationKey, defaultValue, null);
    }

    @Override
    public void init() {
        if (nonNull(settingChangeConsumer)) {
            settingChangeConsumer.accept(value);
        }
    }

    @Override
    public String getSerializedValue() {
        return value;
    }

    @Override
    public void deserializeValue(final String value) {
        if (!value.equals(this.value)) {
            setValue(value);
        }
    }

    public void setValue(final String value) {
        this.value = value;
        if (nonNull(getSettingChangeConsumer())) {
            try {
                getSettingChangeConsumer().accept(value);
            } catch (final Exception e) {
                XaeroPlus.LOGGER.warn("Error applying setting change consumer for {}", getSettingName(), e);
            }
        }
    }

    public Consumer<String> getSettingChangeConsumer() {
        return settingChangeConsumer;
    }

    public void setSettingChangeConsumer(final Consumer<String> settingChangeConsumer) {
        this.settingChangeConsumer = settingChangeConsumer;
    }

    @Override
    public XaeroPlusSettingEntry<?> toXaeroSettingEntry() {
        return null;
    }
}
