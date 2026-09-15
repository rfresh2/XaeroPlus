package xaeroplus.settings;

import com.mojang.blaze3d.platform.InputConstants;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import xaero.lib.common.gui.widget.TooltipInfo;
import xaeroplus.XaeroPlus;
import xaeroplus.feature.extensions.IXaeroPlusSettingEntry;
import xaeroplus.feature.extensions.XaeroPlusCustomSettingEntry;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

public class BooleanSetting extends XaeroPlusSetting {
    public static final KeyMapping.Category KEYBIND_CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("xaeroplus", "keybindings"));
    private boolean value;
    private BooleanConsumer settingChangeConsumer;

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String settingName;
        private String settingNameTranslationKey;
        private Boolean defaultValue;
        private boolean keybind;
        private BooleanConsumer settingChangeConsumer;
        private BooleanSupplier visibilitySupplier;

        private Builder() {}

        public Builder name(String settingName) {
            this.settingName = requireNonNull(settingName, "settingName");
            return this;
        }

        public Builder translationKey(String settingNameTranslationKey) {
            this.settingNameTranslationKey = requireNonNull(settingNameTranslationKey, "settingNameTranslationKey");
            return this;
        }

        public Builder defaultValue(boolean defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder keybind() {
            this.keybind = true;
            return this;
        }

        public Builder onChange(BooleanConsumer settingChangeConsumer) {
            this.settingChangeConsumer = requireNonNull(settingChangeConsumer, "settingChangeConsumer");
            return this;
        }

        public Builder visibleWhen(BooleanSupplier visibilitySupplier) {
            this.visibilitySupplier = requireNonNull(visibilitySupplier, "visibilitySupplier");
            return this;
        }

        public BooleanSetting build() {
            var name = requireNonNull(settingName, "settingName");
            var translationKey = requireNonNull(settingNameTranslationKey, "settingNameTranslationKey");
            return new BooleanSetting(
                SETTING_PREFIX + name,
                translationKey,
                buildTooltipTranslationKey(translationKey),
                keybind ? new KeyMapping(translationKey, InputConstants.UNKNOWN.getValue(), KEYBIND_CATEGORY) : null,
                requireNonNull(defaultValue, "defaultValue"),
                settingChangeConsumer,
                visibilitySupplier
            );
        }
    }

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
    public IXaeroPlusSettingEntry toXaeroSettingEntry() {
        return new XaeroPlusCustomSettingEntry<Boolean>(
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
                Minecraft.getInstance().gui.setScreen(Minecraft.getInstance().gui.screen());
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
