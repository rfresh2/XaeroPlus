package xaeroplus.settings;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.KeyMapping;
import xaero.common.settings.ModOptions;
import xaero.map.gui.CursorBox;
import xaeroplus.XaeroPlus;
import xaeroplus.mixin.client.AccessorMinimapModOptions;
import xaeroplus.mixin.client.AccessorWorldMapModOptions;

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
        return new BooleanSetting(
            SETTING_PREFIX + settingName,
            settingNameTranslationKey,
            buildTooltipTranslationKey(settingNameTranslationKey),
            new KeyMapping(settingNameTranslationKey, -1, "XaeroPlus"),
            defaultValue,
            null, null
        );
    }

    public static BooleanSetting create(String settingName,
                                        String settingNameTranslationKey,
                                        boolean defaultValue,
                                        BooleanConsumer settingChangeConsumer) {
        return new BooleanSetting(
            SETTING_PREFIX + settingName,
            settingNameTranslationKey,
            buildTooltipTranslationKey(settingNameTranslationKey),
            new KeyMapping(settingNameTranslationKey, -1, "XaeroPlus"), defaultValue,
            settingChangeConsumer, null
        );
    }

    public static BooleanSetting create(String settingName,
                                        String settingNameTranslationKey,
                                        boolean defaultValue,
                                        BooleanSupplier visibilitySupplier) {
        return new BooleanSetting(
            SETTING_PREFIX + settingName,
            settingNameTranslationKey,
            buildTooltipTranslationKey(settingNameTranslationKey),
            new KeyMapping(settingNameTranslationKey, -1, "XaeroPlus"), defaultValue,
            null, visibilitySupplier
        );
    }

    public static BooleanSetting create(String settingName,
                                        String settingNameTranslationKey,
                                        boolean defaultValue,
                                        BooleanConsumer settingChangeConsumer,
                                        BooleanSupplier visibilitySupplier) {
        return new BooleanSetting(
            SETTING_PREFIX + settingName,
            settingNameTranslationKey,
            buildTooltipTranslationKey(settingNameTranslationKey),
            new KeyMapping(settingNameTranslationKey, -1, "XaeroPlus"), defaultValue,
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
    public ModOptions toMinimapModOptions() {
        return AccessorMinimapModOptions.createBooleanSetting(
            getSettingName(),
            new xaero.common.graphics.CursorBox(getTooltipTranslationKey()),
            isIngameOnly());
    }

    @Override
    public xaero.map.settings.ModOptions toWorldMapModOptions() {
        return AccessorWorldMapModOptions.createBooleanSetting(
            getSettingName(),
            new CursorBox(getTooltipTranslationKey()),
            isIngameOnly(),
            isRequiresMinimap(),
            false);
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
