package xaeroplus.settings;

import net.minecraft.client.resources.I18n;

public interface TranslatableSettingEnum {
    String getTranslationKey();

    default String getTranslatedName() {
        return I18n.format(getTranslationKey());
    }
}
