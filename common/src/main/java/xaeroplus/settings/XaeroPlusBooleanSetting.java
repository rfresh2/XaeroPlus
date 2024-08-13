package xaeroplus.settings;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.KeyMapping;
import xaeroplus.XaeroPlus;
import xaeroplus.settings.XaeroPlusSettingsReflectionHax.SettingLocation;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import static java.util.Objects.nonNull;

public class XaeroPlusBooleanSetting extends XaeroPlusSetting {

    private boolean value;
    private Consumer<Boolean> settingChangeConsumer;

    private XaeroPlusBooleanSetting(final String settingName,
                                    final String settingNameTranslationKey,
                                    final String tooltipTranslationKey,
                                    final KeyMapping keyBinding,
                                    final boolean value,
                                    final BooleanConsumer settingChangeConsumer,
                                    final BooleanSupplier visibilitySupplier) {
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
            tooltipTranslationKey, new KeyMapping(settingNameTranslationKey, -1, "XaeroPlus"), defaultValue,
            null, null
        );
        settingLocation.getSettingsList().add(setting);
        return setting;
    }

    public static XaeroPlusBooleanSetting create(String settingName,
                                                 String settingNameTranslationKey,
                                                 String tooltipTranslationKey,
                                                 boolean defaultValue,
                                                 BooleanConsumer settingChangeConsumer,
                                                 final SettingLocation settingLocation) {
        final XaeroPlusBooleanSetting setting = new XaeroPlusBooleanSetting(
            SETTING_PREFIX + settingName,
            settingNameTranslationKey,
            tooltipTranslationKey, new KeyMapping(settingNameTranslationKey, -1, "XaeroPlus"), defaultValue,
            settingChangeConsumer, null
        );
        settingLocation.getSettingsList().add(setting);
        return setting;
    }

    public static XaeroPlusBooleanSetting create(String settingName,
                                                 String settingNameTranslationKey,
                                                 String tooltipTranslationKey,
                                                 boolean defaultValue,
                                                 BooleanSupplier visibilitySupplier,
                                                 final SettingLocation settingLocation) {
        final XaeroPlusBooleanSetting setting = new XaeroPlusBooleanSetting(
            SETTING_PREFIX + settingName,
            settingNameTranslationKey,
            tooltipTranslationKey, new KeyMapping(settingNameTranslationKey, -1, "XaeroPlus"), defaultValue,
            null, visibilitySupplier
        );
        settingLocation.getSettingsList().add(setting);
        return setting;
    }

    public static XaeroPlusBooleanSetting create(String settingName,
                                                 String settingNameTranslationKey,
                                                 String tooltipTranslationKey,
                                                 boolean defaultValue,
                                                 BooleanConsumer settingChangeConsumer,
                                                 BooleanSupplier visibilitySupplier,
                                                 final SettingLocation settingLocation) {
        final XaeroPlusBooleanSetting setting = new XaeroPlusBooleanSetting(
            SETTING_PREFIX + settingName,
            settingNameTranslationKey,
            tooltipTranslationKey, new KeyMapping(settingNameTranslationKey, -1, "XaeroPlus"), defaultValue,
            settingChangeConsumer, visibilitySupplier
        );
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
