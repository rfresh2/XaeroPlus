package xaeroplus.settings;

import net.minecraft.client.option.KeyBinding;
import xaeroplus.XaeroPlus;
import xaeroplus.settings.XaeroPlusSettingsReflectionHax.SettingLocation;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.Objects.nonNull;

public class XaeroPlusBooleanSetting extends XaeroPlusSetting {

    private boolean value;
    private Consumer<Boolean> settingChangeConsumer;

    private XaeroPlusBooleanSetting(final String settingName,
                                    final String settingNameTranslationKey,
                                    final boolean value,
                                    final String tooltipTranslationKey,
                                    final Supplier<Boolean> visibilitySupplier,
                                    final Consumer<Boolean> settingChangeConsumer,
                                    final KeyBinding keyBinding) {
        super(settingName, settingNameTranslationKey, tooltipTranslationKey, keyBinding, visibilitySupplier);
        this.value = value;
        this.settingChangeConsumer = settingChangeConsumer;
    }

    public static XaeroPlusBooleanSetting create(String settingName,
                                                 String settingNameTranslationKey,
                                                 String tooltipTranslationKey,
                                                 boolean defaultValue,
                                                 final SettingLocation settingLocation) {
        final XaeroPlusBooleanSetting setting = new XaeroPlusBooleanSetting(
                SETTING_PREFIX + settingName,
                settingNameTranslationKey,
                defaultValue,
                tooltipTranslationKey,
                null,
                null,
                new KeyBinding(settingName, -1, "XaeroPlus"));
        settingLocation.getSettingsList().add(setting);
        return setting;
    }

    public static XaeroPlusBooleanSetting create(String settingName,
                                                 String settingNameTranslationKey,
                                                 String tooltipTranslationKey,
                                                 Consumer<Boolean> settingChangeConsumer,
                                                 boolean defaultValue,
                                                 final SettingLocation settingLocation) {
        final XaeroPlusBooleanSetting setting = new XaeroPlusBooleanSetting(
                SETTING_PREFIX + settingName,
                settingNameTranslationKey,
                defaultValue,
                tooltipTranslationKey,
                null,
                settingChangeConsumer,
                new KeyBinding(settingName, -1, "XaeroPlus"));
        settingLocation.getSettingsList().add(setting);
        return setting;
    }

    public static XaeroPlusBooleanSetting create(String settingName,
                                                 String settingNameTranslationKey,
                                                 String tooltipTranslationKey,
                                                 Supplier<Boolean> visibilitySupplier,
                                                 boolean defaultValue,
                                                 final SettingLocation settingLocation) {
        final XaeroPlusBooleanSetting setting = new XaeroPlusBooleanSetting(
                SETTING_PREFIX + settingName,
                settingNameTranslationKey,
                defaultValue,
                tooltipTranslationKey,
                visibilitySupplier,
                null,
                new KeyBinding(settingName, -1, "XaeroPlus"));
        settingLocation.getSettingsList().add(setting);
        return setting;
    }

    public static XaeroPlusBooleanSetting create(String settingName,
                                                 String settingNameTranslationKey,
                                                 String tooltipTranslationKey,
                                                 Supplier<Boolean> visibilitySupplier,
                                                 Consumer<Boolean> settingChangeConsumer,
                                                 boolean defaultValue,
                                                 final SettingLocation settingLocation) {
        final XaeroPlusBooleanSetting setting = new XaeroPlusBooleanSetting(
                SETTING_PREFIX + settingName,
                settingNameTranslationKey,
                defaultValue,
                tooltipTranslationKey,
                visibilitySupplier,
                settingChangeConsumer,
                new KeyBinding(settingName, -1, "XaeroPlus"));
        settingLocation.getSettingsList().add(setting);
        return setting;
    }

    public boolean getValue() {
        return value;
    }

    public void setValue(final boolean value) {
        this.value = value;
        if (nonNull(getSettingChangeConsumer())) {
            try {
                getSettingChangeConsumer().accept(value);
            } catch (final Exception e) {
                XaeroPlus.LOGGER.warn("Error applying setting change consumer for {}", getSettingName(), e);
            }
        }
    }

    public Consumer<Boolean> getSettingChangeConsumer() {
        return settingChangeConsumer;
    }

    public void setSettingChangeConsumer(final Consumer<Boolean> settingChangeConsumer) {
        this.settingChangeConsumer = settingChangeConsumer;
    }

    @Override
    public void init() {
        if (nonNull(settingChangeConsumer)) {
            settingChangeConsumer.accept(value);
        }
    }

}
