package xaeroplus.settings;

import net.minecraft.client.gui.screens.Screen;
import xaeroplus.XaeroPlus;
import xaeroplus.feature.extensions.IXaeroPlusSettingEntry;
import xaeroplus.feature.extensions.XaeroPlusScreenSwitchSettingEntry;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import static java.util.Objects.nonNull;

public class StringSetting extends XaeroPlusSetting {
    private String value;
    private Consumer<String> settingChangeConsumer;
    private ScreenSupplier screenSupplier;

    public StringSetting(
        final String settingName,
        final String settingNameTranslationKey,
        final String tooltipTranslationKey,
        final String defaultValue,
        final Consumer<String> settingChangeConsumer,
        final BooleanSupplier visibilitySupplier,
        final ScreenSupplier screenSupplier
    ) {
        super(settingName, settingNameTranslationKey, tooltipTranslationKey, null, visibilitySupplier);
        this.value = defaultValue;
        this.settingChangeConsumer = settingChangeConsumer;
        this.screenSupplier = screenSupplier;
    }

    public static StringSetting create(
        String settingName,
        String settingNameTranslationKey,
        String defaultValue,
        Consumer<String> settingChangeConsumer,
        ScreenSupplier screenSupplier,
        BooleanSupplier visibilitySupplier
    ) {
        return new StringSetting(
            SETTING_PREFIX + settingName,
            settingNameTranslationKey,
            buildTooltipTranslationKey(settingNameTranslationKey),
            defaultValue,
            settingChangeConsumer,
            visibilitySupplier,
            screenSupplier
        );
    }

    public static StringSetting create(
        String settingName,
        String settingNameTranslationKey,
        String defaultValue,
        Consumer<String> settingChangeConsumer,
        ScreenSupplier screenSupplier
    ) {
        return create(settingName, settingNameTranslationKey, defaultValue, settingChangeConsumer, screenSupplier, () -> true);
    }

    public static StringSetting create(
        String settingName,
        String settingNameTranslationKey,
        String defaultValue,
        ScreenSupplier screenSupplier
    ) {
        return create(settingName, settingNameTranslationKey, defaultValue, null, screenSupplier, () -> true);
    }

    public static StringSetting create(
        String settingName,
        String settingNameTranslationKey,
        String defaultValue,
        ScreenSupplier screenSupplier,
        BooleanSupplier visibilitySupplier
    ) {
        return create(settingName, settingNameTranslationKey, defaultValue, null, screenSupplier, visibilitySupplier);
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

    public String get() {
        return value;
    }

    public Consumer<String> getSettingChangeConsumer() {
        return settingChangeConsumer;
    }

    public void setSettingChangeConsumer(final Consumer<String> settingChangeConsumer) {
        this.settingChangeConsumer = settingChangeConsumer;
    }

    public ScreenSupplier getScreenSupplier() {
        return screenSupplier;
    }

    @Override
    public IXaeroPlusSettingEntry toXaeroSettingEntry() {
        return new XaeroPlusScreenSwitchSettingEntry(this);
    }

    public interface ScreenSupplier {
        Screen getScreen(Screen parent, Screen escape, StringSetting setting);
    }
}
