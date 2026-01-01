package xaeroplus.settings;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.text.TextComponentString;
import xaero.lib.common.gui.widget.TooltipInfo;
import xaeroplus.XaeroPlus;
import xaeroplus.feature.extensions.XaeroPlusSettingEntry;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import static java.util.Objects.nonNull;

public class BooleanSetting extends XaeroPlusSetting {

    private boolean value;
    private Consumer<Boolean> settingChangeConsumer;

    private BooleanSetting(
        final String settingName,
        final String settingNameTranslationKey,
        final String tooltipTranslationKey,
        final KeyBinding keyBinding,
        final BooleanSupplier visibilitySupplier,
        final Consumer<Boolean> settingChangeConsumer,
        final boolean value
    ) {
        super(settingName, settingNameTranslationKey, tooltipTranslationKey, keyBinding, visibilitySupplier);
        this.value = value;
        this.settingChangeConsumer = settingChangeConsumer;
    }

    public static BooleanSetting create(
        String settingName,
        String settingNameTranslationKey,
        String tooltipTranslationKey,
        boolean defaultValue
    ) {
        return new BooleanSetting(
            SETTING_PREFIX + settingName,
            settingNameTranslationKey,
            tooltipTranslationKey,
            new KeyBinding(settingName, 0, "XaeroPlus"),
            null,
            null,
            defaultValue
        );
    }

    public static BooleanSetting create(
        String settingName,
        String settingNameTranslationKey,
        String tooltipTranslationKey,
        BooleanSupplier visibilitySupplier,
        boolean defaultValue
    ) {
        return new BooleanSetting(
            SETTING_PREFIX + settingName,
            settingNameTranslationKey,
            tooltipTranslationKey,
            new KeyBinding(settingName, 0, "XaeroPlus"),
            visibilitySupplier,
            null,
            defaultValue
        );
    }

    public static BooleanSetting create(
        String settingName,
        String settingNameTranslationKey,
        String tooltipTranslationKey,
        Consumer<Boolean> settingChangeConsumer,
        boolean defaultValue
    ) {
        return new BooleanSetting(
            SETTING_PREFIX + settingName,
            settingNameTranslationKey,
            tooltipTranslationKey,
            new KeyBinding(settingName, 0, "XaeroPlus"),
            null,
            settingChangeConsumer,
            defaultValue
        );
    }

    public static BooleanSetting create(
        String settingName,
        String settingNameTranslationKey,
        String tooltipTranslationKey,
        BooleanSupplier visibilitySupplier,
        Consumer<Boolean> settingChangeConsumer,
        boolean defaultValue
    ) {
        return new BooleanSetting(
            SETTING_PREFIX + settingName,
            settingNameTranslationKey,
            tooltipTranslationKey,
            new KeyBinding(settingName, 0, "XaeroPlus"),
            visibilitySupplier,
            settingChangeConsumer,
            defaultValue
        );
    }

    @Override
    public String getSerializedValue() {
        return Boolean.toString(value);
    }

    @Override
    public void deserializeValue(final String value) {
        boolean b = Boolean.parseBoolean(value);
        if (b != getValue()) setValue(b);
    }

    @Override
    public XaeroPlusSettingEntry<?> toXaeroSettingEntry() {
        return new XaeroPlusSettingEntry<Boolean>(
            this,
            new TextComponentString(getTranslatedName()),
            new TooltipInfo(getTooltipTranslationKey()),
            false,
            this::getValue,
            0,
            1,
            v -> v == 1,
            v -> new TextComponentString(I18n.format(v ? "gui.xaero_on" : "gui.xaero_off")),
            (v1, v2) -> {
                setValue(v2);
                SettingHooks.saveSettings();
                Minecraft.getMinecraft().displayGuiScreen(Minecraft.getMinecraft().currentScreen);
            },
            this::isVisible
        );
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
