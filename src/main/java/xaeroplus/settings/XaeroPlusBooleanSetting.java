package xaeroplus.settings;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import xaeroplus.XaeroPlus;
import xaeroplus.settings.XaeroPlusSettingsReflectionHax.SettingLocation;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.Objects.nonNull;

public class XaeroPlusBooleanSetting extends XaeroPlusSetting {

    private boolean value;
    private Consumer<Boolean> settingChangeConsumer;

    private XaeroPlusBooleanSetting(final String settingName,
                                    final boolean value,
                                    final Text tooltip,
                                    final Supplier<Boolean> visibilitySupplier,
                                    final Consumer<Boolean> settingChangeConsumer,
                                    final KeyBinding keyBinding) {
        super(settingName, tooltip, keyBinding, visibilitySupplier);
        this.value = value;
        this.settingChangeConsumer = settingChangeConsumer;
    }

    public static XaeroPlusBooleanSetting create(String settingName,
                                                 String tooltip,
                                                 boolean defaultValue,
                                                 final SettingLocation settingLocation) {
        final XaeroPlusBooleanSetting setting = new XaeroPlusBooleanSetting(
                SETTING_PREFIX + settingName,
                defaultValue,
                Text.of(tooltip),
                null,
                null,
                new KeyBinding(settingName, -1, "XaeroPlus"));
        settingLocation.getSettingsList().add(setting);
        return setting;
    }

    public static XaeroPlusBooleanSetting create(String settingName,
                                                 String tooltip,
                                                 Consumer<Boolean> settingChangeConsumer,
                                                 boolean defaultValue,
                                                 final SettingLocation settingLocation) {
        final XaeroPlusBooleanSetting setting = new XaeroPlusBooleanSetting(
                SETTING_PREFIX + settingName,
                defaultValue,
                Text.of(tooltip),
                null,
                settingChangeConsumer,
                new KeyBinding(settingName, -1, "XaeroPlus"));
        settingLocation.getSettingsList().add(setting);
        return setting;
    }

    public static XaeroPlusBooleanSetting create(String settingName,
                                                 String tooltip,
                                                 Supplier<Boolean> visibilitySupplier,
                                                 Consumer<Boolean> settingChangeConsumer,
                                                 boolean defaultValue,
                                                 final SettingLocation settingLocation) {
        final XaeroPlusBooleanSetting setting = new XaeroPlusBooleanSetting(SETTING_PREFIX + settingName,
                defaultValue,
                Text.of(tooltip),
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
