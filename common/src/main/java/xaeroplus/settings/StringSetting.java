package xaeroplus.settings;

import net.minecraft.client.gui.screens.Screen;
import xaeroplus.XaeroPlus;
import xaeroplus.feature.extensions.IXaeroPlusSettingEntry;
import xaeroplus.feature.extensions.XaeroPlusScreenSwitchSettingEntry;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import static java.util.Objects.nonNull;

public class StringSetting extends XaeroPlusSetting {
    private String value;
    private Consumer<String> settingChangeConsumer;
    private ScreenSupplier screenSupplier;

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String settingName;
        private String settingNameTranslationKey;
        private String defaultValue;
        private Consumer<String> settingChangeConsumer;
        private BooleanSupplier visibilitySupplier;
        private ScreenSupplier screenSupplier;

        private Builder() {}

        public Builder name(String settingName) {
            this.settingName = Objects.requireNonNull(settingName, "settingName");
            return this;
        }

        public Builder translationKey(String settingNameTranslationKey) {
            this.settingNameTranslationKey = Objects.requireNonNull(settingNameTranslationKey, "settingNameTranslationKey");
            return this;
        }

        public Builder defaultValue(String defaultValue) {
            this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue");
            return this;
        }

        public Builder onChange(Consumer<String> settingChangeConsumer) {
            this.settingChangeConsumer = Objects.requireNonNull(settingChangeConsumer, "settingChangeConsumer");
            return this;
        }

        public Builder visibleWhen(BooleanSupplier visibilitySupplier) {
            this.visibilitySupplier = Objects.requireNonNull(visibilitySupplier, "visibilitySupplier");
            return this;
        }

        public Builder screen(ScreenSupplier screenSupplier) {
            this.screenSupplier = Objects.requireNonNull(screenSupplier, "screenSupplier");
            return this;
        }

        public StringSetting build() {
            var name = Objects.requireNonNull(settingName, "settingName");
            var translationKey = Objects.requireNonNull(settingNameTranslationKey, "settingNameTranslationKey");
            return new StringSetting(
                SETTING_PREFIX + name,
                translationKey,
                buildTooltipTranslationKey(translationKey),
                Objects.requireNonNull(defaultValue, "defaultValue"),
                settingChangeConsumer,
                visibilitySupplier,
                Objects.requireNonNull(screenSupplier, "screenSupplier")
            );
        }
    }

    private StringSetting(
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
