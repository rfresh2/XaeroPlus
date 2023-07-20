package xaeroplus.settings;

import net.minecraft.client.settings.KeyBinding;
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
                                    final String tooltipTranslationKey,
                                    final KeyBinding keyBinding,
                                    final Supplier<Boolean> visibilitySupplier,
                                    final Consumer<Boolean> settingChangeConsumer,
                                    final boolean value) {
        super(settingName, settingNameTranslationKey, tooltipTranslationKey, keyBinding, visibilitySupplier);
        this.value = value;
        this.settingChangeConsumer = settingChangeConsumer;
    }

    public static XaeroPlusBooleanSetting create(String settingName,
                                                 String settingNameTranslationKey,
                                                 String tooltipTranslationKey,
                                                 boolean defaultValue,
                                                 final SettingLocation settingLocation) {
        final XaeroPlusBooleanSetting setting = new XaeroPlusBooleanSetting(SETTING_PREFIX + settingName,
                                                                            settingNameTranslationKey,
                                                                            tooltipTranslationKey,
                                                                            new KeyBinding(settingName, 0, "XaeroPlus"),
                                                                            null,
                                                                            null,
                                                                            defaultValue
        );
        settingLocation.getSettingsList().add(setting);
        return setting;
    }

    public static XaeroPlusBooleanSetting create(String settingName,
                                                 String settingNameTranslationKey,
                                                 String tooltipTranslationKey,
                                                 Supplier<Boolean> visibilitySupplier,
                                                 boolean defaultValue,
                                                 final SettingLocation settingLocation) {
        final XaeroPlusBooleanSetting setting = new XaeroPlusBooleanSetting(SETTING_PREFIX + settingName,
                                                                            settingNameTranslationKey,
                                                                            tooltipTranslationKey,
                                                                            new KeyBinding(settingName, 0, "XaeroPlus"),
                                                                            visibilitySupplier,
                                                                            null,
                                                                            defaultValue
        );
        settingLocation.getSettingsList().add(setting);
        return setting;
    }

    public static XaeroPlusBooleanSetting create(String settingName,
                                                 String settingNameTranslationKey,
                                                 String tooltipTranslationKey,
                                                 Consumer<Boolean> settingChangeConsumer,
                                                 boolean defaultValue,
                                                 final SettingLocation settingLocation) {
        final XaeroPlusBooleanSetting setting = new XaeroPlusBooleanSetting(SETTING_PREFIX + settingName,
                                                                            settingNameTranslationKey,
                                                                            tooltipTranslationKey,
                                                                            new KeyBinding(settingName, 0, "XaeroPlus"),
                                                                            null,
                                                                            settingChangeConsumer,
                                                                            defaultValue
        );
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
        final XaeroPlusBooleanSetting setting = new XaeroPlusBooleanSetting(SETTING_PREFIX + settingName,
                                                                            settingNameTranslationKey,
                                                                            tooltipTranslationKey,
                                                                            new KeyBinding(settingName, 0, "XaeroPlus"),
                                                                            visibilitySupplier,
                                                                            settingChangeConsumer,
                                                                            defaultValue
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
