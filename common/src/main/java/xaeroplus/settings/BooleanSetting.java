package xaeroplus.settings;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import xaero.lib.common.gui.widget.TooltipInfo;
import xaeroplus.XaeroPlus;
import xaeroplus.feature.extensions.XaeroPlusSettingEntry;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import static java.util.Objects.nonNull;

public class BooleanSetting extends XaeroPlusSetting {

    private boolean value;
    private BooleanConsumer settingChangeConsumer;

    private BooleanSetting(final String settingName,
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

    public static BooleanSetting create(String settingName,
                                        String settingNameTranslationKey,
                                        boolean defaultValue) {
        return create(settingName, settingNameTranslationKey, defaultValue, false);
    }

    public static BooleanSetting create(String settingName,
                                        String settingNameTranslationKey,
                                        boolean defaultValue,
                                        boolean keybind
    ) {
        return new BooleanSetting(
            SETTING_PREFIX + settingName,
            settingNameTranslationKey,
            buildTooltipTranslationKey(settingNameTranslationKey),
            keybind ? new KeyMapping(settingNameTranslationKey, -1, "XaeroPlus") : null,
            defaultValue,
            null, null
        );
    }

    public static BooleanSetting create(String settingName,
                                        String settingNameTranslationKey,
                                        boolean defaultValue,
                                        BooleanConsumer settingChangeConsumer) {
        return create(settingName, settingNameTranslationKey, defaultValue, false, settingChangeConsumer);
    }

    public static BooleanSetting create(String settingName,
                                        String settingNameTranslationKey,
                                        boolean defaultValue,
                                        boolean keybind,
                                        BooleanConsumer settingChangeConsumer) {
        return new BooleanSetting(
            SETTING_PREFIX + settingName,
            settingNameTranslationKey,
            buildTooltipTranslationKey(settingNameTranslationKey),
            keybind ? new KeyMapping(settingNameTranslationKey, -1, "XaeroPlus") : null,
            defaultValue,
            settingChangeConsumer, null
        );
    }

    public static BooleanSetting create(String settingName,
                                        String settingNameTranslationKey,
                                        boolean defaultValue,
                                        BooleanSupplier visibilitySupplier) {
        return create(settingName, settingNameTranslationKey, defaultValue, false, visibilitySupplier);
    }

    public static BooleanSetting create(String settingName,
                                        String settingNameTranslationKey,
                                        boolean defaultValue,
                                        boolean keybind,
                                        BooleanSupplier visibilitySupplier
    ) {
        return new BooleanSetting(
            SETTING_PREFIX + settingName,
            settingNameTranslationKey,
            buildTooltipTranslationKey(settingNameTranslationKey),
            keybind ? new KeyMapping(settingNameTranslationKey, -1, "XaeroPlus") : null,
            defaultValue,
            null, visibilitySupplier
        );
    }

    public static BooleanSetting create(String settingName,
                                        String settingNameTranslationKey,
                                        boolean defaultValue,
                                        BooleanConsumer settingChangeConsumer,
                                        BooleanSupplier visibilitySupplier) {
        return create(settingName, settingNameTranslationKey, defaultValue, false, settingChangeConsumer, visibilitySupplier);
    }

    public static BooleanSetting create(String settingName,
                                        String settingNameTranslationKey,
                                        boolean defaultValue,
                                        boolean keybind,
                                        BooleanConsumer settingChangeConsumer,
                                        BooleanSupplier visibilitySupplier
    ) {
        return new BooleanSetting(
            SETTING_PREFIX + settingName,
            settingNameTranslationKey,
            buildTooltipTranslationKey(settingNameTranslationKey),
            keybind ? new KeyMapping(settingNameTranslationKey, -1, "XaeroPlus") : null,
            defaultValue,
            settingChangeConsumer, visibilitySupplier
        );
    }

    @Override
    public String getSerializedValue() {
        return Boolean.toString(value);
    }

    @Override
    public void deserializeValue(String value) {
        var v = Boolean.parseBoolean(value);
        if (v != get()) setValue(v);
    }

    @Override
    public XaeroPlusSettingEntry<?> toXaeroSettingEntry() {
        return new XaeroPlusSettingEntry<Boolean>(
            this,
            Component.literal(getTranslatedName()),
            new TooltipInfo(getTooltipTranslationKey()),
            false,
            this::get,
            0,
            1,
            v -> v == 1,
            v -> Component.translatable(v ? "gui.xaero_on" : "gui.xaero_off"),
            (v1, v2) -> {
                setValue(v2);
                SettingHooks.saveSettings();
                Minecraft.getInstance().setScreen(Minecraft.getInstance().screen);
            },
            this::isVisible
        );
    }

    public boolean get() {
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

    public void setSettingChangeConsumer(final BooleanConsumer settingChangeConsumer) {
        this.settingChangeConsumer = settingChangeConsumer;
    }

    @Override
    public void init() {
        if (nonNull(settingChangeConsumer)) {
            settingChangeConsumer.accept(value);
        }
    }

}
